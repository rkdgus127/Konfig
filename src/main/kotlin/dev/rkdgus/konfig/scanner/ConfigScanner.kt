package dev.rkdgus.konfig.scanner

import dev.rkdgus.konfig.annotation.Config
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.logging.Logger
import java.util.jar.JarFile

object ConfigScanner {

    fun scan(plugin: Plugin, logger: Logger): Set<Class<*>> {
        val source = runCatching {
            plugin.javaClass.protectionDomain.codeSource?.location?.toURI()
        }.getOrNull() ?: return emptySet()

        val sourceFile = runCatching {
            File(source)
        }.getOrNull() ?: return emptySet()

        val classNames = when {
            sourceFile.isFile && sourceFile.extension.equals("jar", true) ->
                scanJar(sourceFile)

            sourceFile.isDirectory ->
                scanDirectory(sourceFile)

            else -> emptySet()
        }

        val result = linkedSetOf<Class<*>>()
        val loader = plugin.javaClass.classLoader

        for (name in classNames) {
            val type = try {
                Class.forName(name, false, loader)
            } catch (_: Throwable) {
                continue
            }

            if (hasConfigFields(type)) {
                result += type
            }
        }

        logger.info("Scanned ${plugin.name}: found ${result.size} @Config class(es).")
        return result
    }

    private fun hasConfigFields(type: Class<*>): Boolean {
        var current: Class<*>? = type

        while (current != null && current != Any::class.java) {
            if (current.declaredFields.any {
                    it.isAnnotationPresent(Config::class.java)
                }) {
                return true
            }

            current = current.superclass
        }

        return false
    }

    private fun scanJar(file: File): Set<String> {
        val names = linkedSetOf<String>()

        JarFile(file).use { jar ->
            val entries = jar.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val path = entry.name

                if (!entry.isDirectory &&
                    path.endsWith(".class") &&
                    !path.startsWith("META-INF/") &&
                    path != "module-info.class" &&
                    !path.endsWith("package-info.class")
                ) {
                    names += path.removeSuffix(".class").replace('/', '.')
                }
            }
        }

        return names
    }

    private fun scanDirectory(directory: File): Set<String> {
        val names = linkedSetOf<String>()

        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { file ->
                val relative = file.relativeTo(directory).invariantSeparatorsPath

                if (relative != "module-info.class" &&
                    !relative.endsWith("package-info.class")
                ) {
                    names += relative.removeSuffix(".class").replace('/', '.')
                }
            }

        return names
    }
}