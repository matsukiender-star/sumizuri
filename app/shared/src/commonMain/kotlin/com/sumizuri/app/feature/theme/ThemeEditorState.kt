package com.sumizuri.app.feature.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemeOptions
import com.sumizuri.app.ui.theme.spec.ThemeShapes
import kotlin.random.Random

// What the editor holds while a theme is being changed: the theme as it is now and as it was when the editor opened, the changes that can be undone or done again, the open tab, the piece in focus and the mode the preview is shown in.
internal class ThemeEditorState(val original: AppThemeSpec, startDark: Boolean) {
    var draft by mutableStateOf(original)
        private set
    var section by mutableStateOf(EditorSection.Basics)
        private set
    var focus by mutableStateOf<EditorPart?>(null)
        private set

    // Goes up each time the preview should scroll to the piece in focus.
    var reveal by mutableIntStateOf(0)
        private set
    var dark by mutableStateOf(startDark)
    var comparing by mutableStateOf(false)

    // Whether the preview is laid out as in a desktop window, or null to follow the width of the window.
    var desktop by mutableStateOf<Boolean?>(null)

    // Lists the buttons watch, so they know when they can be used.
    private val undone = mutableStateListOf<AppThemeSpec>()
    private val redone = mutableStateListOf<AppThemeSpec>()
    val canUndo: Boolean get() = undone.isNotEmpty()
    val canRedo: Boolean get() = redone.isNotEmpty()
    val changed: Boolean get() = draft != original

    // The theme from before a slider or the color picker started to show its change, which is what an undo goes back to once the change is kept.
    private var beforeTrying: AppThemeSpec? = null

    // What the last change was to, such as the name. Letters typed one after another make one change that is undone at once, not one for each letter.
    private var lastGroup: String? = null

    // Opens a tab, and lets the preview scroll to the piece the tab is about.
    fun open(next: EditorSection) {
        section = next
        focus = next.firstPart
        reveal++
    }

    // Opens the tab of a piece tapped on the preview, which is in view already.
    fun select(part: EditorPart) {
        section = part.section
        focus = part
    }

    // Takes a new version of the theme, which can be undone.
    fun change(next: AppThemeSpec, group: String? = null) {
        val before = beforeTrying ?: draft
        beforeTrying = null
        if (next == before) {
            draft = next
            return
        }
        if (group == null || group != lastGroup || undone.isEmpty()) undone.add(before)
        lastGroup = group
        redone.clear()
        draft = next
    }

    // Shows a version of the theme while a slider moves or a color is chosen, without making it a step of its own. The change that follows, or a cancel, ends it.
    fun tryOut(next: AppThemeSpec) {
        if (beforeTrying == null) beforeTrying = draft
        draft = next
    }

    // Puts the theme back to how it was before it was tried out.
    fun cancelTry() {
        beforeTrying?.let { draft = it }
        beforeTrying = null
    }

    fun options(edit: (ThemeOptions) -> ThemeOptions) = change(draft.copy(options = edit(draft.options)))

    fun shapes(edit: (ThemeShapes) -> ThemeShapes) = change(draft.copy(shapes = edit(draft.shapes)))

    fun undo() {
        cancelTry()
        val last = undone.removeLastOrNull() ?: return
        redone.add(draft)
        draft = last
        lastGroup = null
    }

    fun redo() {
        cancelTry()
        val next = redone.removeLastOrNull() ?: return
        undone.add(draft)
        draft = next
        lastGroup = null
    }

    // Takes the whole theme back to how it was when the editor opened, as one change that can be undone.
    fun resetAll() = change(original.copy(name = draft.name, author = draft.author))

    // Takes what one tab sets back to how it was when the editor opened.
    fun reset(section: EditorSection) = change(resetOf(section))

    // Whether what a tab sets differs from how it was when the editor opened, so its reset has something to do.
    fun sectionChanged(section: EditorSection): Boolean = resetOf(section) != draft

    // The theme as it is now with what one tab sets as it was when the editor opened.
    private fun resetOf(section: EditorSection): AppThemeSpec {
        val was = original
        val now = draft
        return when (section) {
            EditorSection.Basics -> now.copy(
                shapes = was.shapes,
                options = now.options.copy(effects = now.options.effects.copy(oledDark = was.options.effects.oledDark)),
            )

            EditorSection.Colors -> now.copy(light = was.light, dark = was.dark)

            EditorSection.Text -> now.copy(
                options = now.options.copy(typography = was.options.typography, layout = was.options.layout),
            )

            EditorSection.Covers -> now.copy(
                shapes = now.shapes.copy(cover = was.shapes.cover),
                options = now.options.copy(
                    covers = was.options.covers,
                    pageColors = was.options.pageColors,
                    effects = now.options.effects.copy(coverShadow = was.options.effects.coverShadow),
                ),
            )

            EditorSection.Lists -> now.copy(
                shapes = now.shapes.copy(
                    card = was.shapes.card,
                    item = was.shapes.item,
                    iconTile = was.shapes.iconTile,
                ),
                options = now.options.copy(components = was.options.components),
            )

            EditorSection.Pieces -> now.copy(
                shapes = now.shapes.copy(
                    chip = was.shapes.chip,
                    dialog = was.shapes.dialog,
                    navBar = was.shapes.navBar,
                ),
                options = now.options.copy(controls = was.options.controls),
            )

            EditorSection.Motion -> now.copy(options = now.options.copy(motion = was.options.motion))

            EditorSection.Backdrop -> now.copy(
                options = now.options.copy(
                    background = was.options.background,
                    effects = now.options.effects.copy(bloom = was.options.effects.bloom),
                ),
            )
        }
    }
}

// A fresh id for a theme a person makes, which no other theme has.
fun newThemeId(): String = Random.nextLong().toULong().toString(36)
