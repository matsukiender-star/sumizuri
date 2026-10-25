package com.sumizuri.app.ui.text

import androidx.compose.runtime.Composable
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.lang_ar
import com.sumizuri.app.shared.resources.lang_de
import com.sumizuri.app.shared.resources.lang_en
import com.sumizuri.app.shared.resources.lang_es
import com.sumizuri.app.shared.resources.lang_fr
import com.sumizuri.app.shared.resources.lang_id
import com.sumizuri.app.shared.resources.lang_it
import com.sumizuri.app.shared.resources.lang_ja
import com.sumizuri.app.shared.resources.lang_ko
import com.sumizuri.app.shared.resources.lang_local
import com.sumizuri.app.shared.resources.lang_multi
import com.sumizuri.app.shared.resources.lang_pt
import com.sumizuri.app.shared.resources.lang_ru
import com.sumizuri.app.shared.resources.lang_th
import com.sumizuri.app.shared.resources.lang_tr
import com.sumizuri.app.shared.resources.lang_vi
import com.sumizuri.app.shared.resources.lang_zh
import org.jetbrains.compose.resources.stringResource

// A language code as a person would say it. A code the app has no name for shows as capital letters.
@Composable
fun languageName(code: String): String = when (code.lowercase().substringBefore('-')) {
    "local" -> stringResource(Res.string.lang_local)
    "all", "multi", "mul" -> stringResource(Res.string.lang_multi)
    "en" -> stringResource(Res.string.lang_en)
    "ja" -> stringResource(Res.string.lang_ja)
    "ko" -> stringResource(Res.string.lang_ko)
    "zh" -> stringResource(Res.string.lang_zh)
    "es" -> stringResource(Res.string.lang_es)
    "pt" -> stringResource(Res.string.lang_pt)
    "fr" -> stringResource(Res.string.lang_fr)
    "de" -> stringResource(Res.string.lang_de)
    "it" -> stringResource(Res.string.lang_it)
    "ru" -> stringResource(Res.string.lang_ru)
    "ar" -> stringResource(Res.string.lang_ar)
    "id" -> stringResource(Res.string.lang_id)
    "th" -> stringResource(Res.string.lang_th)
    "tr" -> stringResource(Res.string.lang_tr)
    "vi" -> stringResource(Res.string.lang_vi)
    else -> code.uppercase()
}
