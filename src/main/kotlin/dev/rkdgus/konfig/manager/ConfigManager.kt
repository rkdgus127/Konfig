package dev.rkdgus.konfig.manager

import dev.rkdgus.konfig.annotation.Config
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ConfigManager(private val host: Plugin) {
    private val bindings = ConcurrentHashMap<File, ConfigBinding>()

    fun register(plugin: Plugin, type: Class<*>) {
        val fields = collectFields(type)
        if (fields.isEmpty()) return

        val pluginFolder = File(host.dataFolder.parentFile, plugin.name)
        val file = File(pluginFolder, "${type.simpleName}.yml").absoluteFile.normalize()

        val binding = ConfigBinding(plugin, type, fields, file)
        if (bindings.putIfAbsent(file, binding) != null) return

        try {
            if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
                throw IllegalStateException("Could not create ${pluginFolder.absolutePath}")
            }

            if (fields.all { Modifier.isStatic(it.modifiers) }) {
                load(binding, true)
            }

            host.logger.info("Konfig: registered ${plugin.name}/${file.name}")
        } catch (exception: Exception) {
            bindings.remove(file)
            host.logger.warning("Konfig: failed to register ${type.name}: ${exception.message}")
        }
    }

    fun bind(plugin: Plugin, instance: Any) {
        bindings.values
            .filter { it.plugin === plugin && it.type.isInstance(instance) }
            .forEach { binding ->
                if (binding.instances.addIfAbsent(instance)) {
                    try {
                        load(binding, true)
                        host.logger.info(
                            "Konfig: bound ${plugin.name}/${binding.type.simpleName}"
                        )
                    } catch (exception: Exception) {
                        host.logger.warning(
                            "Konfig: failed to bind ${binding.type.name}: ${exception.message}"
                        )
                    }
                }
            }
    }

    fun unbind(plugin: Plugin, instance: Any) {
        bindings.values
            .filter { it.plugin === plugin }
            .forEach { it.instances.remove(instance) }
    }

    fun reload(file: File) {
        val binding = bindings[file.absoluteFile.normalize()] ?: return

        try {
            load(binding, false)
            host.logger.info("Konfig: reloaded ${binding.plugin.name}/${file.name}")
        } catch (exception: Exception) {
            host.logger.warning("Konfig: could not reload ${file.name}: ${exception.message}")
        }
    }

    fun unregister(plugin: Plugin) {
        bindings.entries.removeIf { it.value.plugin === plugin }
    }

    fun files(): Set<File> = bindings.keys

    private fun load(binding: ConfigBinding, createMissing: Boolean) {
        val yaml = YamlConfiguration()

        if (binding.file.exists()) {
            yaml.load(binding.file)
        }

        var changed = false

        for (field in binding.fields) {
            field.isAccessible = true
            val key = field.name

            if (yaml.contains(key)) {
                val converted = convert(yaml.get(key), field.type)

                if (!converted.first) {
                    host.logger.warning(
                        "Konfig: unsupported value for ${binding.type.simpleName}.$key (${field.type.name})"
                    )
                    continue
                }

                if (Modifier.isStatic(field.modifiers)) {
                    field.set(null, converted.second)
                } else {
                    binding.instances.forEach {
                        field.set(it, converted.second)
                    }
                }
            } else if (createMissing) {
                val defaultValue = if (Modifier.isStatic(field.modifiers)) {
                    field.get(null)
                } else {
                    binding.instances.firstOrNull()?.let { field.get(it) }
                }

                if (defaultValue != null) {
                    yaml.set(key, serialize(defaultValue))
                    changed = true
                }
            }
        }

        if (changed || !binding.file.exists()) {
            yaml.save(binding.file)
        }
    }

    private fun collectFields(type: Class<*>): List<Field> {
        val result = mutableListOf<Field>()
        var current: Class<*>? = type

        while (current != null && current != Any::class.java) {
            current.declaredFields
                .filter {
                    it.isAnnotationPresent(Config::class.java) &&
                            !it.isSynthetic &&
                            !Modifier.isFinal(it.modifiers)
                }
                .forEach(result::add)

            current = current.superclass
        }

        return result
    }

    private fun serialize(value: Any): Any = when (value) {
        is Enum<*> -> value.name
        is UUID -> value.toString()
        is Set<*> -> value.toList()
        else -> value
    }

    private fun convert(value: Any?, target: Class<*>): Pair<Boolean, Any?> {
        if (value == null) {
            return if (target.isPrimitive) false to null else true to null
        }

        return try {
            when {
                target.isInstance(value) -> true to value
                target == String::class.java -> true to value.toString()

                target == Int::class.javaPrimitiveType ||
                        target == Int::class.javaObjectType ->
                    true to (value as Number).toInt()

                target == Long::class.javaPrimitiveType ||
                        target == Long::class.javaObjectType ->
                    true to (value as Number).toLong()

                target == Double::class.javaPrimitiveType ||
                        target == Double::class.javaObjectType ->
                    true to (value as Number).toDouble()

                target == Float::class.javaPrimitiveType ||
                        target == Float::class.javaObjectType ->
                    true to (value as Number).toFloat()

                target == Short::class.javaPrimitiveType ||
                        target == Short::class.javaObjectType ->
                    true to (value as Number).toShort()

                target == Byte::class.javaPrimitiveType ||
                        target == Byte::class.javaObjectType ->
                    true to (value as Number).toByte()

                target == Boolean::class.javaPrimitiveType ||
                        target == Boolean::class.javaObjectType ->
                    true to value.toString().toBooleanStrict()

                target == UUID::class.java ->
                    true to UUID.fromString(value.toString())

                target.isEnum -> {
                    val enumValue = target.enumConstants.firstOrNull {
                        (it as Enum<*>).name.equals(value.toString(), true)
                    }

                    (enumValue != null) to enumValue
                }

                List::class.java.isAssignableFrom(target) ->
                    true to (value as? List<*>)?.toMutableList()

                Set::class.java.isAssignableFrom(target) ->
                    true to (value as? List<*>)?.toSet()

                Map::class.java.isAssignableFrom(target) ->
                    true to (value as? Map<*, *>)?.toMap()

                else -> false to null
            }
        } catch (_: Exception) {
            false to null
        }
    }

    private data class ConfigBinding(
        val plugin: Plugin,
        val type: Class<*>,
        val fields: List<Field>,
        val file: File,
        val instances: CopyOnWriteArrayList<Any> = CopyOnWriteArrayList()
    )
}