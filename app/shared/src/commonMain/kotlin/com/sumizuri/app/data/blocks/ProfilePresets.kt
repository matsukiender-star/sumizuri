package com.sumizuri.app.data.blocks

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.preset_classic
import com.sumizuri.app.shared.resources.preset_classic_about
import com.sumizuri.app.shared.resources.preset_dashboard
import com.sumizuri.app.shared.resources.preset_dashboard_about
import com.sumizuri.app.shared.resources.preset_minimal
import com.sumizuri.app.shared.resources.preset_minimal_about
import com.sumizuri.app.shared.resources.preset_reader
import com.sumizuri.app.shared.resources.preset_reader_about
import org.jetbrains.compose.resources.StringResource

// A block of a preset: the template it starts from and the size it takes, where a height of 0 fits its content. The rows a block that fits its content is drawn with in the sketch of the preset are a guess, as its real height is only known on the page.
class PresetPart(val template: BlockTemplate, val w: Int, val h: Int, val sketchRows: Int = if (h > 0) h else 2)

// Whole profile pages to start from. Classic is the page as it came. Choosing one replaces the blocks of the page.
enum class ProfilePreset(val label: StringResource, val about: StringResource, val parts: List<PresetPart>) {
    Classic(
        Res.string.preset_classic,
        Res.string.preset_classic_about,
        listOf(
            PresetPart(BlockTemplate.Figures, 4, 0),
            PresetPart(BlockTemplate.Year, 4, 0),
            PresetPart(BlockTemplate.Continue, 4, 0, sketchRows = 1),
            PresetPart(BlockTemplate.Statistics, 4, 0, sketchRows = 3),
        ),
    ),
    Dashboard(
        Res.string.preset_dashboard,
        Res.string.preset_dashboard_about,
        listOf(
            PresetPart(BlockTemplate.Streak, 2, 1),
            PresetPart(BlockTemplate.Pace, 2, 1),
            PresetPart(BlockTemplate.Goal, 4, 2),
            PresetPart(BlockTemplate.Genres, 4, 0, sketchRows = 3),
            PresetPart(BlockTemplate.Weekdays, 4, 0),
        ),
    ),
    Minimal(
        Res.string.preset_minimal,
        Res.string.preset_minimal_about,
        listOf(
            PresetPart(BlockTemplate.Streak, 2, 1),
            PresetPart(BlockTemplate.Finished, 2, 1),
            PresetPart(BlockTemplate.Recent, 4, 0),
        ),
    ),
    Reader(
        Res.string.preset_reader,
        Res.string.preset_reader_about,
        listOf(
            PresetPart(BlockTemplate.Continue, 4, 0, sketchRows = 1),
            PresetPart(BlockTemplate.Recent, 4, 0),
            PresetPart(BlockTemplate.Heatmap, 4, 0),
            PresetPart(BlockTemplate.MostRead, 4, 0),
        ),
    ),
    ;

    // The page of the preset. Classic gives the page as it came, with the blocks it always had.
    suspend fun layout(): BlockLayout = if (this ==
        Classic
    ) {
        defaultLayout()
    } else {
        BlockLayout(blocks = parts.map { ProfileBlock(newBlockId(), it.w, it.h, it.template.script()) })
    }
}
