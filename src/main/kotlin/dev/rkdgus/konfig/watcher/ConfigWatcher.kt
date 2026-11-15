package dev.rkdgus.konfig.watcher

import dev.rkdgus.konfig.manager.ConfigManager
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class ConfigWatcher(
    private val plugin: Plugin,
    private val manager: ConfigManager
) : AutoCloseable {

    private val watchService: WatchService =
        FileSystems.getDefault().newWatchService()

    private val keys = ConcurrentHashMap<WatchKey, File>()

    @Volatile
    private var running = false

    private var worker: Thread? = null

    fun start() {
        if (running) return

        running = true
        refreshDirectories()

        worker = thread(
            name = "Konfig-FileWatcher",
            isDaemon = true
        ) {
            while (running) {
                val key = try {
                    watchService.take()
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    break
                }

                val directory = keys[key]

                if (directory != null) {
                    val changed = mutableSetOf<File>()

                    for (event in key.pollEvents()) {
                        val relative =
                            event.context() as? java.nio.file.Path ?: continue

                        val file = File(directory, relative.toString())

                        if (file.extension.equals("yml", true)) {
                            changed += file.absoluteFile.normalize()
                        }
                    }

                    if (changed.isNotEmpty()) {
                        Thread.sleep(250)

                        changed.forEach { file ->
                            plugin.server.scheduler.runTask(
                                plugin,
                                Runnable {
                                    if (file.isFile) {
                                        manager.reload(file)
                                    }
                                }
                            )
                        }
                    }
                }

                if (!key.reset()) {
                    keys.remove(key)
                }
            }
        }
    }

    fun refreshDirectories() {
        val pluginsFolder = plugin.dataFolder.parentFile ?: return
        if (!pluginsFolder.isDirectory) return

        pluginsFolder.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { directory ->
                val alreadyRegistered = keys.values.any {
                    it.absoluteFile.normalize() ==
                            directory.absoluteFile.normalize()
                }

                if (!alreadyRegistered) {
                    val key = directory.toPath().register(
                        watchService,
                        ENTRY_CREATE,
                        ENTRY_MODIFY,
                        ENTRY_DELETE
                    )

                    keys[key] = directory
                }
            }
    }

    override fun close() {
        running = false
        worker?.interrupt()
        watchService.close()
        keys.clear()
    }
}