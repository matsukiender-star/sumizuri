package com.sumizuri.app.feature.reader.lookup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.lookup_credit
import com.sumizuri.app.shared.resources.lookup_failed
import com.sumizuri.app.shared.resources.lookup_missing
import com.sumizuri.app.shared.resources.lookup_search_web
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.dock.PanelHead
import org.jetbrains.compose.resources.stringResource

// The meaning of a word as a panel the dock grows to hold. The result is null while the dictionary is being asked.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupPanel(word: String, result: LookupResult?, onSearchWeb: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelHead(word, onDismiss)
        when (result) {
            null -> InkCircularProgressIndicator(Modifier.padding(vertical = 8.dp))

            is LookupResult.Found -> {
                for (entry in result.entries) EntryBlock(entry, entry.word != word)
                Text(
                    stringResource(Res.string.lookup_credit, result.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LookupResult.Missing -> Text(
                stringResource(Res.string.lookup_missing),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LookupResult.Failed -> Text(
                stringResource(Res.string.lookup_failed),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onSearchWeb) { Text(stringResource(Res.string.lookup_search_web)) }
        }
    }
}

// One entry: the word with how it is read, and its meanings under the kind of word they are for.
@Composable
private fun EntryBlock(entry: DictionaryEntry, showWord: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showWord || entry.reading != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                if (showWord) {
                    Text(
                        entry.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                entry.reading?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        for (meaning in entry.meanings) {
            if (meaning.partOfSpeech.isNotBlank()) {
                Text(
                    meaning.partOfSpeech,
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            meaning.definitions.forEachIndexed { index, line ->
                Text(
                    if (meaning.definitions.size >
                        1
                    ) {
                        "${index + 1}. $line"
                    } else {
                        line
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
