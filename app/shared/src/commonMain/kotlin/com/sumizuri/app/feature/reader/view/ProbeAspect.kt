package com.sumizuri.app.feature.reader.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.SuccessResult
import coil3.size.Size
import com.sumizuri.app.ui.kit.image.PageImages
import com.sumizuri.app.ui.kit.image.buildImageRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit

// The picture is read at a small size, which is enough to see its shape. What comes in is kept in the disk cache, so the page then shows at once.
private const val ProbeSize = 256

// Reads a page and says its height over its width, or null when it could not be read. A page ahead of the reader waits for its turn with the other work ahead.
suspend fun probeAspect(
    context: PlatformContext,
    url: String,
    headers: Map<String, String>,
    ahead: Boolean = false,
): Double? {
    val request = buildImageRequest(context, url, headers).newBuilder().size(Size(ProbeSize, ProbeSize)).build()
    val answer = if (ahead) {
        PageWork.ahead.withPermit {
            PageImages.get(context).execute(request)
        }
    } else {
        PageImages.get(context).execute(request)
    }
    val image = (answer as? SuccessResult)?.image
    return image?.takeIf { it.width > 0 && it.height > 0 }?.let { it.height.toDouble() / it.width }
}

// Looks at the shape of several pages at once and hands over the ones that could be read.
@Composable
fun ProbeAspect(urls: List<String>, headers: Map<String, String>, onAspects: (List<Double>) -> Unit) {
    val context = LocalPlatformContext.current
    LaunchedEffect(urls) {
        val shapes = coroutineScope { urls.map { url -> async { probeAspect(context, url, headers) } }.awaitAll() }
        onAspects(shapes.filterNotNull())
    }
}
