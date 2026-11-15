package dev.rkdgus.konfig

import dev.rkdgus.konfig.manager.ConfigManager
import dev.rkdgus.konfig.scanner.ConfigScanner
import dev.rkdgus.konfig.watcher.ConfigWatcher
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import rkdgus.Event.ListenerRegisterEvent
import rkdgus.Event.ListenerUnregisterEvent

class KonfigPlugin : JavaPlugin(), Listener {

    private lateinit var configManager: ConfigManager
    private lateinit var watcher: ConfigWatcher

    override fun onEnable() {
        configManager = ConfigManager(this)
        watcher = ConfigWatcher(this, configManager)

        server.pluginManager.registerEvents(this, this)

        server.scheduler.runTask(this, Runnable {
            scanAllPlugins()
            watcher.start()
        })
    }

    override fun onDisable() {
        if (::watcher.isInitialized) {
            watcher.close()
        }
    }

    @EventHandler
    fun onListenerRegister(event: ListenerRegisterEvent) {
        configManager.bind(event.plugin, event.listener)
    }

    @EventHandler
    fun onListenerUnregister(event: ListenerUnregisterEvent) {
        configManager.unbind(event.plugin, event.listener)
    }

    @EventHandler
    fun onPluginEnable(event: PluginEnableEvent) {
        if (event.plugin === this) return

        server.scheduler.runTask(this, Runnable {
            scanPlugin(event.plugin)
        })
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin === this) return

        configManager.unregister(event.plugin)
    }

    private fun scanAllPlugins() {
        server.pluginManager.plugins
            .filter { it !== this && it.isEnabled }
            .forEach(::scanPlugin)
    }

    private fun scanPlugin(plugin: Plugin) {
        ConfigScanner.scan(plugin, logger).forEach {
            configManager.register(plugin, it)
        }

        findRegisteredListeners(plugin).forEach {
            configManager.bind(plugin, it)
        }

        watcher.refreshDirectories()
    }

    private fun findRegisteredListeners(plugin: Plugin): List<Any> {
        val autoListen = server.pluginManager.getPlugin("AutoListen")
            ?: return emptyList()

        if (!autoListen.isEnabled) return emptyList()

        return try {
            val method = autoListen.javaClass.getMethod(
                "getRegistered",
                Plugin::class.java
            )

            val result = method.invoke(autoListen, plugin) as? List<*>
                ?: return emptyList()

            result.filterNotNull()
        } catch (exception: Exception) {
            logger.warning(
                "Konfig: could not retrieve AutoListen instances: ${exception.message}"
            )
            emptyList()
        }
    }
}