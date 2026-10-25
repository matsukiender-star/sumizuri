package com.sumizuri.app.feature.reader.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_open_browser
import com.sumizuri.app.shared.resources.reader_page_copy_link
import com.sumizuri.app.shared.resources.reader_page_menu_title
import com.sumizuri.app.shared.resources.reader_page_save
import com.sumizuri.app.shared.resources.reader_page_set_cover
import com.sumizuri.app.shared.resources.reader_page_share
import com.sumizuri.app.ui.kit.dock.PanelHead
import com.sumizuri.app.ui.kit.image.PageImages
import com.sumizuri.app.ui.kit.image.buildImageRequest
import com.sumizuri.app.ui.kit.input.canShareFiles
import com.sumizuri.app.ui.kit.input.rememberCopyText
import com.sumizuri.app.ui.kit.input.rememberFileSaver
import com.sumizuri.app.ui.kit.input.rememberFileSharer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.stringResource

// The extensions a saved picture can have, and the one used when the address does not say.
private val Extensions = setOf("jpg", "jpeg", "png", "webp", "gif", "avif")
private const val UsualExtension = "jpg"

// What a long press on a page offers, as a panel the dock grows to hold: saving or sharing the picture as a file, making it the cover of the title, and copying its address. The picture is taken from the copy the reader already holds, so a page that is not kept on the device cannot be saved or shared. The cover is not offered when it is null, as for a page that is shuffled.
@Composable
fun PageMenuPanel(
    number: Int,
    address: String,
    onSetCover: (() -> Unit)?,
    onOpenBrowser: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val copy = rememberCopyText()
    val share = rememberFileSharer()
    val made = remember { mutableStateOf<Path?>(null) }
    val saveFile = rememberFileSaver { _ ->
        made.value?.let { runCatching { FileSystem.SYSTEM.delete(it) } }
        onDismiss()
    }
    val saveable by produceState(false, address) {
        value = withContext(Dispatchers.IO) {
            val snapshot = PageImages.get(context).diskCache?.openSnapshot(address)
            snapshot?.close()
            snapshot != null
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PanelHead(stringResource(Res.string.reader_page_menu_title, number), onDismiss)
        // The page itself, when it is kept on the device, so it is clear which picture the actions are for.
        if (saveable) {
            val request = remember(address) { buildImageRequest(context, address) }
            AsyncImage(
                request,
                null,
                PageImages.get(context),
                Modifier.padding(start = 10.dp, bottom = 8.dp).height(120.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            if (saveable) {
                MenuRow(Icons.Outlined.Download, stringResource(Res.string.reader_page_save)) {
                    scope.launch {
                        val name = "page-$number.${extensionOf(address)}"
                        val file = withContext(Dispatchers.IO) { copyOut(context, address, name) }
                        if (file == null) {
                            onDismiss()
                        } else {
                            made.value = file
                            saveFile(name, file)
                        }
                    }
                }
            }
            if (saveable && canShareFiles()) {
                MenuRow(Icons.Outlined.Share, stringResource(Res.string.reader_page_share)) {
                    scope.launch {
                        val name = "page-$number.${extensionOf(address)}"
                        val file = withContext(Dispatchers.IO) { copyOut(context, address, name) }
                        if (file != null) share(name, file)
                        onDismiss()
                    }
                }
            }
            if (onSetCover != null) {
                MenuRow(Icons.Outlined.Image, stringResource(Res.string.reader_page_set_cover)) {
                    onSetCover()
                    onDismiss()
                }
            }
            MenuRow(Icons.Outlined.Link, stringResource(Res.string.reader_page_copy_link)) {
                copy(address)
                onDismiss()
            }
            if (onOpenBrowser != null) {
                MenuRow(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.reader_open_browser)) {
                    onOpenBrowser()
                    onDismiss()
                }
            }
        }
    }
}

@Composable
// One action of the panel, its icon in the accent beside its name.
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                MaterialTheme.shapes.medium,
            ).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun extensionOf(address: String): String {
    val name = address.substringBefore('?').substringBefore('#').substringAfterLast('/')
    val extension = name.substringAfterLast('.', "").lowercase()
    return if (extension in Extensions) extension else UsualExtension
}

// Copies the kept file of a picture to a file of its own in the temporary folder, or gives null when it is not kept.
private fun copyOut(context: coil3.PlatformContext, address: String, name: String): Path? {
    val cache = PageImages.get(context).diskCache ?: return null
    val snapshot = cache.openSnapshot(address) ?: return null
    return try {
        val target = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
            .resolve("sumizuri_save")
            .also { FileSystem.SYSTEM.createDirectories(it) }
            .resolve(name)
        FileSystem.SYSTEM
            .source(snapshot.data)
            .buffer()
            .use { from ->
                FileSystem.SYSTEM
                    .sink(target)
                    .buffer()
                    .use { to -> to.writeAll(from) }
            }
        target
    } catch (_: Exception) {
        null
    } finally {
        snapshot.close()
    }
}
