package com.sumizuri.app.feature.tv.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.data.pair.PhotoDrop
import com.sumizuri.app.data.pair.openPhotoDrop
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.feature.onboarding.keepAvatar
import com.sumizuri.app.feature.tv.onboarding.firstFocus
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.shared.resources.suki_tv_page_done
import com.sumizuri.app.shared.resources.suki_tv_page_failed
import com.sumizuri.app.shared.resources.suki_tv_page_send
import com.sumizuri.app.shared.resources.suki_tv_page_sending
import com.sumizuri.app.shared.resources.suki_tv_page_text
import com.sumizuri.app.shared.resources.suki_tv_photo_failed
import com.sumizuri.app.shared.resources.suki_tv_photo_hint
import com.sumizuri.app.shared.resources.suki_tv_photo_no_network
import com.sumizuri.app.shared.resources.suki_tv_photo_title
import com.sumizuri.app.shared.resources.suki_tv_photo_waiting
import com.sumizuri.app.ui.tv.TvButton
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

// A code a phone on the same network scans to send a photo for a profile. The photo is squared and kept as it comes, and handed back as the path of the picture.
@Composable
internal fun TvPhotoDrop(onPhoto: (String) -> Unit, onClose: () -> Unit) {
    val host = LocalProfiles.current
    val page = photoPage()
    val photo by rememberUpdatedState(onPhoto)
    var drop by remember { mutableStateOf<PhotoDrop?>(null) }
    var opened by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = true) { onClose() }
    // Closing the listener is what frees the wait for a phone when the panel goes.
    DisposableEffect(drop) { onDispose { drop?.close() } }
    LaunchedEffect(Unit) {
        val made = withContext(Dispatchers.IO) { openPhotoDrop(page) }
        drop = made
        opened = true
        while (made != null) {
            val bytes = runCatching { made.receive() }.getOrNull() ?: return@LaunchedEffect
            val kept = keepAvatar(host, bytes)
            if (kept != null) return@LaunchedEffect photo(kept)
            failed = true
        }
    }
    val first = firstFocus()
    Row(
        Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(Res.string.suki_tv_photo_title), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            val ready = drop != null
            if (opened) {
                Text(
                    stringResource(if (ready) Res.string.suki_tv_photo_hint else Res.string.suki_tv_photo_no_network),
                    Modifier.widthIn(max = 520.dp),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ready) {
                Text(
                    stringResource(if (failed) Res.string.suki_tv_photo_failed else Res.string.suki_tv_photo_waiting),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            TvButton(
                stringResource(Res.string.cancel),
                onClose,
                Modifier.padding(top = 10.dp).focusRequester(first),
                main = false,
            )
        }
        drop?.let { ready ->
            Box(Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Image(rememberQrCodePainter(ready.url), null, Modifier.size(260.dp))
            }
        }
    }
}

// The page the phone opens: a picture to pick, shrunk to a square on the phone so little goes over the network, and a button that sends it.
@Composable
private fun photoPage(): String {
    val title = stringResource(Res.string.suki_tv_photo_title)
    val text = stringResource(Res.string.suki_tv_page_text)
    val send = stringResource(Res.string.suki_tv_page_send)
    val sending = stringResource(Res.string.suki_tv_page_sending)
    val done = stringResource(Res.string.suki_tv_page_done)
    val failed = stringResource(Res.string.suki_tv_page_failed)
    return remember(title, text, send, sending, done, failed) {
        """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Sumizuri</title><style>
        body{margin:0;font-family:system-ui,sans-serif;background:#0f1114;color:#f3f4f6;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;box-sizing:border-box}
        .card{width:100%;max-width:420px;background:#1a1d23;border-radius:20px;padding:24px;text-align:center}
        h1{font-size:21px;margin:0 0 8px}p{color:#9ca3af;font-size:14px;line-height:1.45;margin:0 0 18px}
        img{width:140px;height:140px;border-radius:50%;object-fit:cover;display:none;margin:0 auto 16px}
        input{width:100%;margin-bottom:14px;color:#d1d5db}
        button{width:100%;padding:14px;border:0;border-radius:12px;background:#d4a64a;color:#111;font-weight:700;font-size:16px}
        button:disabled{opacity:.45}#msg{margin-top:14px;font-size:14px}
        </style></head><body><div class="card">
        <h1>${html(title)}</h1><p>${html(text)}</p><img id="face" alt="">
        <input id="pick" type="file" accept="image/*"><button id="send" disabled>${html(send)}</button>
        <div id="msg" data-sending="${html(sending)}" data-done="${html(done)}" data-failed="${html(failed)}"></div>
        </div><script>
        const pick=document.getElementById('pick'),send=document.getElementById('send'),face=document.getElementById('face'),msg=document.getElementById('msg');
        let blob=null;
        pick.onchange=()=>{const f=pick.files&&pick.files[0];if(!f)return;const i=new Image();i.onload=()=>{
        const c=document.createElement('canvas');c.width=c.height=512;const m=Math.min(i.width,i.height);
        c.getContext('2d').drawImage(i,(i.width-m)/2,(i.height-m)/2,m,m,0,0,512,512);
        face.src=c.toDataURL('image/jpeg',.88);face.style.display='block';
        c.toBlob(b=>{blob=b;send.disabled=!b;msg.textContent=''},'image/jpeg',.88)};i.src=URL.createObjectURL(f)};
        send.onclick=async()=>{if(!blob)return;send.disabled=true;msg.textContent=msg.dataset.sending;
        try{const r=await fetch(location.pathname,{method:'POST',body:blob});msg.textContent=r.ok?msg.dataset.done:msg.dataset.failed}
        catch(e){msg.textContent=msg.dataset.failed}send.disabled=false};
        </script></body></html>
        """.trimIndent()
    }
}

private fun html(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
