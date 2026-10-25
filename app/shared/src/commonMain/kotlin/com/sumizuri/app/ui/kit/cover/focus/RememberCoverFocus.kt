package com.sumizuri.app.ui.kit.cover.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import com.sumizuri.app.ui.kit.cover.CoverMemo
import com.sumizuri.app.ui.kit.image.buildImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The cover is read this many pixels wide, which is enough for the cascade to see a face and is quick.
private const val ReadWidth = 256

// Where each cover was found to have its face, so a cover is only looked at once while the app runs. It is used from the main thread only.
private val found = HashMap<String, CoverFocus>()

// A cover is looked at again when the level changes, so each level keeps its own answer.
private fun keyOf(url: String, level: FaceLevel) = "$level $url"

// Looks at the cover and says where its face is, or null when it cannot be read.
suspend fun findCoverFocus(context: PlatformContext, url: String, level: FaceLevel): CoverFocus? {
    val key = keyOf(url, level)
    found[key]?.let { return it }
    // A cover looked at in an earlier run is not looked at again.
    CoverMemo
        .get("focus:$key")
        ?.split(",")
        ?.mapNotNull {
            it.toFloatOrNull()
        }?.takeIf { it.size == 3 }
        ?.let { (x, y, aspect) ->
            return CoverFocus(x, y, aspect).also { found[key] = it }
        }
    val request = buildImageRequest(context, url)
        .newBuilder()
        .size(Size(Dimension(ReadWidth), Dimension.Undefined))
        .cpuReadable()
        .build()
    val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image ?: return null
    val pixels = withContext(Dispatchers.Default) { image.argbPixels() } ?: return null
    val focus = withContext(Dispatchers.Default) { focusOfCover(pixels, level) }
    found[key] = focus
    CoverMemo.put("focus:$key", "${focus.x},${focus.y},${focus.aspect}")
    return focus
}

// The face of a cover as the page learns it, null until it is known.
@Composable
fun rememberCoverFocus(url: String?): State<CoverFocus?> {
    val context = LocalPlatformContext.current
    val level = LocalFaceLevel.current
    return produceState<CoverFocus?>(url?.let { found[keyOf(it, level)] }, url, level) {
        value = if (url.isNullOrBlank()) null else findCoverFocus(context, url, level)
    }
}

// Takes in a face found in an earlier run, under the same key the finder uses, so the backdrop has its place on the first frame.
internal fun keepFocus(key: String, saved: String) {
    val parts = saved.split(",").mapNotNull { it.toFloatOrNull() }
    if (parts.size == 3) found.getOrPut(key) { CoverFocus(parts[0], parts[1], parts[2]) }
}
