package com.sumizuri.app.feature.settings.system

import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.sumizuri.app.core.perf.Perf
import com.sumizuri.app.core.perf.PerfWatch
import com.sumizuri.app.data.AppContainer
import com.sumizuri.app.ui.kit.image.PageCache
import com.sumizuri.app.ui.text.byteSizeText

// The lines the advanced level of the performance overlay adds: the open screen, what the database holds, what runs right now and the pictures kept in memory and on disk.
fun perfDetails(container: AppContainer, context: PlatformContext): suspend () -> List<String> = {
    val tables = container.storage.tables()
    val loader = SingletonImageLoader.get(context)
    val memory = loader.memoryCache
    val disk = loader.diskCache
    val (pages, pageBytes) = PageCache.usage()
    val running = container.manager.runningIds
    buildList {
        add("screen ${PerfWatch.screen}")
        add("db ${byteSizeText(tables.bytes)}  titles ${tables.titles}, ${tables.inLibrary} saved")
        add("chapters ${tables.chapters}  marks ${tables.marks}  logs ${tables.logs}")
        add("sources ${tables.sources}, ${running.size} running")
        if (running.isNotEmpty()) {
            add(
                "  " + running
                    .take(
                        4,
                    ).joinToString(", ") + if (running.size > 4) ", and ${running.size - 4} more" else "",
            )
        }
        add("net ${Perf.requests.value} now  browser pages ${container.manager.browserPages()}")
        if (memory !=
            null
        ) {
            add(
                "images ${byteSizeText(memory.size)} of ${byteSizeText(memory.maxSize)}" +
                    (disk?.let { "  disk ${byteSizeText(it.size)}" } ?: ""),
            )
        }
        add("reader pages $pages, ${byteSizeText(pageBytes)}")
    }
}
