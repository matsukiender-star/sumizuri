package com.sumizuri.app.feature.migration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.migration.MigrationSession
import com.sumizuri.app.data.migration.MigrationState
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.mig_subtitle
import com.sumizuri.app.shared.resources.mig_title
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.page.PageTopBar
import com.sumizuri.app.ui.kit.page.ReadableColumn
import com.sumizuri.app.ui.kit.setting.PartTabs
import org.jetbrains.compose.resources.stringResource

// The page for moving many titles at once. It has four parts under tabs: the titles that are in the work, the sources that are searched and in which order, the rules for matching and moving, and what was found. A bar under all of them starts, stops and moves.
@Composable
internal fun MigrationScreen(
    state: MigrationState,
    shelves: List<Category>,
    session: MigrationSession,
    onBack: () -> Unit,
    onOpenTitle: (Long) -> Unit,
) {
    var tab by remember { mutableStateOf(MigrationTab.Titles) }
    ReadableColumn {
        Column(Modifier.fillMaxSize()) {
            PageTopBar(
                stringResource(Res.string.mig_title),
                subtitle = stringResource(Res.string.mig_subtitle),
                onBack = onBack,
            )
            Column(Modifier.padding(horizontal = PageGutter)) {
                PartTabs(MigrationTab.entries, tab, { stringResource(it.label) }) { tab = it }
            }
            Column(Modifier.weight(1f)) {
                when {
                    !state.loaded -> InkCircularProgressIndicator(Modifier.padding(24.dp))
                    tab == MigrationTab.Titles -> MigrationTitlesTab(state, shelves, session)
                    tab == MigrationTab.Sources -> MigrationSourcesTab(state, session)
                    tab == MigrationTab.Rules -> MigrationRulesTab(state, session)
                    else -> MigrationResultsTab(state, session, onOpenTitle)
                }
            }
            MigrationBar(state, session) { tab = MigrationTab.Results }
        }
    }
}
