package com.sumizuri.app.ui.kit.cover.focus.face

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.shared.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// The cascade for drawn faces, read from the file that comes with the app the first time a face is looked for and kept after that. It is null when the file cannot be read, and then covers fall back to the skin finder.
object FaceCascade {
    private val lock = Mutex()
    private var cascade: LbpCascade? = null
    private var failed = false

    suspend fun get(): LbpCascade? = lock.withLock {
        if (cascade == null && !failed) {
            try {
                cascade = parseLbpCascade(Res.readBytes("files/face/lbpcascade_animeface.xml").decodeToString())
            } catch (error: Exception) {
                failed = true
                AppLog.warn("cover", "the face cascade could not be read, ${error.message}")
            }
        }
        cascade
    }
}

// A face the cascade found: the middle of its square window, the side of it, and how many windows agreed.
class FaceBox(val x: Float, val y: Float, val size: Float, val neighbors: Int)
