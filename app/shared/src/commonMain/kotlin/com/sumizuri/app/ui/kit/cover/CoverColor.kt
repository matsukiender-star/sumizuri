package com.sumizuri.app.ui.kit.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import com.sumizuri.app.ui.kit.cover.focus.argbPixels
import com.sumizuri.app.ui.kit.cover.focus.cpuReadable
import com.sumizuri.app.ui.kit.image.buildImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The cover is read this many pixels wide, which is plenty to find its colors.
private const val ReadWidth = 96

// A palette found for a cover, or none for a grey one.
private class Found(val palette: CoverPalette?)

// Where each cover was looked at, so a cover is only looked at once while the app runs. It is used from the main thread only.
private val looked = HashMap<String, Found>()

// The one color kept for a cover by an older version of the app, shown on the first frame until the whole palette is found.
private val earlier = HashMap<String, CoverPalette>()

// Looks at the cover and gives its palette, or null when it is grey or cannot be read.
suspend fun findCoverPalette(context: PlatformContext, url: String): CoverPalette? {
    looked[url]?.let { return it.palette }
    // A cover looked at in an earlier run is not looked at again, a grey one included.
    CoverMemo.get("palette:$url")?.let { saved ->
        val palette = if (saved == CoverMemo.Nothing) null else CoverPalette.fromText(saved)
        looked[url] = Found(palette)
        return palette
    }
    val request = buildImageRequest(context, url)
        .newBuilder()
        .size(Size(Dimension(ReadWidth), Dimension.Undefined))
        .cpuReadable()
        .build()
    val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image ?: return null
    val pixels = withContext(Dispatchers.Default) { image.argbPixels() } ?: return null
    val palette = withContext(Dispatchers.Default) { coverPaletteOf(pixels) }
    looked[url] = Found(palette)
    CoverMemo.put("palette:$url", palette?.toText() ?: CoverMemo.Nothing)
    return palette
}

// The palette of a cover as the page learns it, null until it is known and for a grey cover.
@Composable
fun rememberCoverPalette(url: String?): State<CoverPalette?> {
    val context = LocalPlatformContext.current
    val first = url?.let { looked[it]?.palette ?: earlier[it] }
    return produceState(first, url) {
        value = if (url.isNullOrBlank()) null else findCoverPalette(context, url)
    }
}

// Takes in a palette found in an earlier run, so a page that opens this cover has its colors on the first frame.
internal fun keepPalette(url: String, saved: String) {
    looked.getOrPut(url) { Found(if (saved == CoverMemo.Nothing) null else CoverPalette.fromText(saved)) }
}

// Takes in the one color an older version kept, which shows until the palette is found.
internal fun keepColor(url: String, saved: String) {
    if (saved == CoverMemo.Nothing) return
    val color = saved.toLongOrNull()?.let { Color(it.toInt()) } ?: return
    earlier.getOrPut(url) { CoverPalette(listOf(color)) }
}
