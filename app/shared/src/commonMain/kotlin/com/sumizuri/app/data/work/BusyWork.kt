package com.sumizuri.app.data.work

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.busy_backup
import com.sumizuri.app.shared.resources.busy_downloads
import com.sumizuri.app.shared.resources.busy_library
import com.sumizuri.app.shared.resources.busy_linking
import com.sumizuri.app.shared.resources.busy_migration
import com.sumizuri.app.shared.resources.busy_restore
import com.sumizuri.app.shared.resources.busy_sources
import com.sumizuri.app.shared.resources.busy_sync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

// The kinds of long work the app keeps going while the person uses another app, each with the words its notice shows.
enum class BusyKind(val label: StringResource) {
    Migration(Res.string.busy_migration),
    Linking(Res.string.busy_linking),
    Library(Res.string.busy_library),
    Downloads(Res.string.busy_downloads),
    Backup(Res.string.busy_backup),
    Restore(Res.string.busy_restore),
    Sources(Res.string.busy_sources),
    Sync(Res.string.busy_sync),
}

// One piece of work that is running: how far it got as done and total when that is known, and what stops it.
class BusyTask internal constructor(
    val id: Long,
    val kind: BusyKind,
    val progress: () -> Pair<Int, Int>?,
    val onStop: () -> Unit,
    val detail: suspend () -> String?,
)

// The long work running now. While there is any, a phone keeps the app awake in the background with a notice that shows how far it got and can stop it, so migrating titles or saving chapters goes on when the person switches to another app.
object BusyWork {
    private val mutable = MutableStateFlow<List<BusyTask>>(emptyList())
    val tasks: StateFlow<List<BusyTask>> = mutable
    private var next = 0L

    // Marks work as running until the handle it gives is ended. detail gives a line more for the notice, such as the source a search is on.
    fun begin(
        kind: BusyKind,
        progress: () -> Pair<Int, Int>? = { null },
        onStop: () -> Unit,
        detail: suspend () -> String? = { null },
    ): BusyHandle {
        val task = BusyTask(next++, kind, progress, onStop, detail)
        mutable.update { it + task }
        BusyPlatform.keepAwake()
        return BusyHandle(task.id)
    }

    // Runs the block as running work, ended when the block ends in any way.
    suspend fun <T> keep(
        kind: BusyKind,
        progress: () -> Pair<Int, Int>? = { null },
        onStop: () -> Unit,
        detail: suspend () -> String? = { null },
        block: suspend () -> T,
    ): T {
        val handle = begin(kind, progress, onStop, detail)
        try {
            return block()
        } finally {
            handle.end()
        }
    }

    // Stops all the work, from the button on the notice.
    fun stopAll() {
        for (task in mutable.value) runCatching { task.onStop() }
    }

    internal fun end(id: Long) = mutable.update { list -> list.filter { it.id != id } }
}

class BusyHandle internal constructor(private val id: Long) {
    fun end() = BusyWork.end(id)
}

// What the system does while work runs: on a phone it starts the notice that keeps the app from being paused in the background. Elsewhere nothing is needed.
expect object BusyPlatform {
    fun keepAwake()
}
