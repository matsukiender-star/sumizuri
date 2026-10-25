package com.sumizuri.app.feature.browse.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_report_summary
import com.sumizuri.app.source.model.FilterGroup
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceInfo
import com.sumizuri.app.source.model.SourcePreference
import com.sumizuri.app.ui.kit.cover.SourceMark
import org.jetbrains.compose.resources.stringResource

// What the code says about itself, with the names the code uses, and which parts it says it has.
internal fun LazyListScope.infoPart(info: SourceInfo, back: (() -> Unit)?) {
    item { PartHeader(info.name, stringResource(TestStep.Info.label), back) }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SourceMark(info.iconUrl.orEmpty(), size = 48.dp)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Field("name", info.name)
                    Field("lang", info.lang)
                    Field("baseUrl", info.baseUrl)
                    Field("iconUrl", info.iconUrl)
                    Field("hasDetails", yes(info.hasDetails))
                    Field("hasFilters", yes(info.hasFilters))
                    Field("hasCatalogs", yes(info.hasCatalogs))
                    Field("hasComments", yes(info.hasComments))
                    Field("hasChapterComments", yes(info.hasChapterComments))
                    Field("hasPreferences", yes(info.hasPreferences))
                }
            }
        }
    }
}

private fun yes(value: Boolean) = if (value) "true" else "false"

// The filters a search can use: each group with its key, its kind, its choices and the one it starts on.
internal fun LazyListScope.filtersPart(filters: List<FilterGroup>, back: (() -> Unit)?) {
    item { PartHeader(stringResource(TestStep.Filters.label), filters.size.toString(), back) }
    items(filters) { group ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                group.name.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SelectionContainer {
                Column {
                    Field("key", group.key)
                    Field("type", group.type.name)
                    Field(
                        "options",
                        (
                            group.options.take(20).joinToString { "${it.label} (${it.value})" } +
                                if (group.options.size > 20) " …" else ""
                            ).ifEmpty { null },
                    )
                    Field("default", group.defaultSelection()?.toString())
                }
            }
        }
    }
}

// The catalogs of the source, each opened as a list.
internal fun LazyListScope.catalogsPart(
    catalogs: List<SourceCatalog>,
    idle: Boolean,
    back: (() -> Unit)?,
    onOpen: (SourceCatalog) -> Unit,
) {
    item { PartHeader(stringResource(TestStep.Catalogs.label), catalogs.size.toString(), back) }
    items(catalogs) { catalog -> ResultRow(catalog.name.ifBlank { "—" }, catalog.id) { if (idle) onOpen(catalog) } }
}

// The settings the source offers a person, with what each starts as and what it is now.
internal fun LazyListScope.settingsPart(
    settings: List<SourcePreference>,
    values: Map<String, Any?>,
    back: (() -> Unit)?,
) {
    item { PartHeader(stringResource(TestStep.Settings.label), settings.size.toString(), back) }
    items(settings) { setting ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                setting.title.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            setting.summary?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            SelectionContainer {
                Column {
                    Field("key", setting.key)
                    Field("type", setting.type.name)
                    Field("default", setting.defaultValue?.toString())
                    Field("value", values[setting.key]?.toString())
                    if (setting.options.isNotEmpty()) {
                        Field(
                            "options",
                            setting.options.joinToString { "${it.label} (${it.value})" },
                        )
                    }
                }
            }
        }
    }
}

// The report of Run all: how many steps worked, gave something to look at, failed or were not tried, then each step with what it gave and its notes. A step that showed something opens it again.
internal fun LazyListScope.reportPart(test: ExtensionTest, onShow: (TestView) -> Unit) {
    val lines = test.report
    item {
        PartHeader(
            stringResource(TestStep.RunAll.label),
            stringResource(
                Res.string.ext_report_summary,
                lines.count { it.outcome == Outcome.Passed },
                lines.count {
                    it.outcome ==
                        Outcome.Warned
                },
                lines.count { it.outcome == Outcome.Failed },
                lines.count {
                    it.outcome ==
                        Outcome.Skipped
                },
            ),
            null,
        )
    }
    items(lines) { line ->
        val view = viewOf(line.step, test).takeIf { line.outcome == Outcome.Passed || line.outcome == Outcome.Warned }
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (view !=
                        null
                    ) {
                        Modifier.clickable { onShow(view) }
                    } else {
                        Modifier
                    },
                ).padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutcomeIcon(line.outcome)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(line.step.label),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    line.ms?.let {
                        Text(
                            "$it ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                SelectionContainer {
                    Text(
                        noteText(line.detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (line.outcome ==
                            Outcome.Failed
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Notes(line.notes)
            }
        }
    }
}

// The view that shows what a step of Run all found, when it is still there to show.
private fun viewOf(step: TestStep, test: ExtensionTest): TestView? = when (step) {
    TestStep.Info -> TestView.Info.takeIf { test.info != null }

    TestStep.Popular, TestStep.Latest, TestStep.Search, TestStep.NextPage, TestStep.Catalog -> TestView.List.takeIf {
        test.list !=
            null
    }

    TestStep.Details -> TestView.Title.takeIf { test.entry != null }

    TestStep.Pages, TestStep.Videos -> TestView.Chapter.takeIf { test.chapter != null }

    TestStep.Comments -> TestView.Comments.takeIf { test.entry != null }

    TestStep.ChapterComments -> TestView.ChapterComments.takeIf { test.chapter != null }

    TestStep.Filters -> TestView.Filters

    TestStep.Catalogs -> TestView.Catalogs

    TestStep.Settings -> TestView.Settings

    else -> null
}

@Composable
private fun OutcomeIcon(outcome: Outcome) {
    val scheme = MaterialTheme.colorScheme
    val (icon, tint) = when (outcome) {
        Outcome.Passed -> Icons.Outlined.CheckCircle to scheme.primary
        Outcome.Warned -> Icons.Outlined.WarningAmber to scheme.tertiary
        Outcome.Failed -> Icons.Outlined.ErrorOutline to scheme.error
        Outcome.Skipped -> Icons.Outlined.RemoveCircleOutline to scheme.outline
    }
    Icon(icon, outcome.name, Modifier.size(20.dp), tint = tint)
}
