package com.sumizuri.app.playback

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// How wide the small pictures are drawn.
private const val ThumbWidth = 240

// A second mpv, quiet and paused, that shows small pictures of the video for the seek bar. It opens the same video with no sound or subtitles, jumps to the key frame nearest the time asked, which needs no decoding of the frames before it, and draws that one frame small. It starts when the first picture is asked for and stops with the player.
internal class MpvThumbnails(private val mpv: MpvApi, private val picture: MutableStateFlow<VideoThumbnail?>) {
    @Volatile private var alive = true
    private var handle: Pointer? = null
    private var context: Pointer? = null
    private var media: PlayerMedia? = null
    private var opened: String? = null

    @Volatile private var asked = -1L
    private val update = Semaphore(0)
    private val wake = MpvUpdate { update.release() }

    // The picture at a time of the video the player plays. A new video drops the picture of the last one.
    @Synchronized
    fun ask(positionMs: Long, playing: PlayerMedia?) {
        if (!alive) return
        val video = playing ?: return
        // A stream in pieces gets no small pictures. Each picture would fetch another piece from the same server through this second mpv, and the log showed servers turning the player away for asking too often.
        if (video.url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)) return
        if (media?.url != video.url) {
            media = video
            picture.value = null
        }
        val here = handle ?: started() ?: return
        if (opened != video.url) {
            mpv.useHeaders(here, video.headers)
            mpv.mpv_set_property_string(here, "start", seconds(positionMs))
            mpv.command(here, "loadfile", video.url, "replace")
            opened = video.url
        } else {
            mpv.command(here, "seek", seconds(positionMs), "absolute+keyframes")
        }
        asked = positionMs
    }

    @Synchronized
    fun close() {
        alive = false
        update.release()
        val here = handle ?: return
        val drawn = context
        handle = null
        context = null
        thread(name = "mpv thumbnails release", isDaemon = true) {
            synchronized(wake) {
                drawn?.let {
                    mpv.mpv_render_context_set_update_callback(it, null, null)
                    mpv.mpv_render_context_free(it)
                }
            }
            mpv.mpv_terminate_destroy(here)
        }
    }

    private fun started(): Pointer? {
        val made = mpv.mpv_create() ?: return null
        val options = listOf(
            "vo" to "libmpv", "terminal" to "no", "config" to "no", "load-scripts" to "no", "ytdl" to "no",
            "idle" to "yes", "pause" to "yes", "keep-open" to "yes", "aid" to "no", "sid" to "no", "audio" to "no",
            "hr-seek" to "no", "hwdec" to "no", "cache" to "yes", "demuxer-max-bytes" to "16MiB",
            "network-timeout" to "20",
        )
        for ((name, value) in options) mpv.mpv_set_option_string(made, name, value)
        val result = PointerByReference()
        if (mpv.mpv_initialize(made) < 0 ||
            mpv.mpv_render_context_create(result, made, Mpv.params(Mpv.ParamApiType to Mpv.text("sw"))) < 0
        ) {
            mpv.mpv_terminate_destroy(made)
            return null
        }
        handle = made
        context = result.value
        mpv.mpv_render_context_set_update_callback(result.value, wake, null)
        thread(name = "mpv thumbnails events", isDaemon = true) { while (alive) mpv.mpv_wait_event(made, 0.5) }
        thread(name = "mpv thumbnails frames", isDaemon = true) { draw(made, result.value) }
        return made
    }

    // Draws each new frame small and hands it over with the time it was asked for.
    private fun draw(here: Pointer, drawn: Pointer) {
        val size = Memory(8)
        val stride = Memory(8)
        val format = Mpv.text("bgr0")
        while (alive) {
            update.tryAcquire(300, TimeUnit.MILLISECONDS)
            synchronized(wake) {
                if (!alive) return
                if (mpv.mpv_render_context_update(drawn) and Mpv.UpdateFrame == 0L) return@synchronized
                val sourceWidth = mpv.number(here, "dwidth")?.toInt() ?: 0
                val sourceHeight = mpv.number(here, "dheight")?.toInt() ?: 0
                if (sourceWidth <= 0 || sourceHeight <= 0) return@synchronized
                val width = ThumbWidth
                val height = (sourceHeight.toLong() * width / sourceWidth).toInt().coerceAtLeast(1)
                val rowBytes = width * 4L
                val memory = Memory(rowBytes * height)
                size.setInt(0, width)
                size.setInt(4, height)
                if (Native.SIZE_T_SIZE == 8) stride.setLong(0, rowBytes) else stride.setInt(0, rowBytes.toInt())
                val params = Mpv.params(
                    Mpv.ParamSwSize to size,
                    Mpv.ParamSwFormat to format,
                    Mpv.ParamSwStride to stride,
                    Mpv.ParamSwPointer to memory,
                )
                if (mpv.mpv_render_context_render(drawn, params) < 0) return@synchronized
                val bytes = memory.getByteArray(0, (rowBytes * height).toInt())
                val image = Image.makeRaster(
                    ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE),
                    bytes,
                    width * 4,
                )
                picture.value = VideoThumbnail(asked, image.toComposeImageBitmap())
                image.close()
            }
        }
    }
}
