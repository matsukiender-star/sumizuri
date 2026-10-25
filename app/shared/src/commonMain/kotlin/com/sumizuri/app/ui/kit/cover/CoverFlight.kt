package com.sumizuri.app.ui.kit.cover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.ui.motion.LocalPageLeaving
import com.sumizuri.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// How long the cover takes to fly to its place on the page of the title, and back to its tile.
private const val OutMs = 420
private const val BackMs = 360

// A page that has not shown its cover by then is not waited for.
private const val WaitMs = 1200L

// A curve that leaves quickly and settles softly, so the cover seems thrown and caught.
private val Glide = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// A cover flying between its tile and its place on the page of the title. A tile keeps where its cover is and a tap flies it from there to where the page shows it. When the page is closed the cover flies back from the page to the tile, which stays empty until it lands. Both places are followed while they move, since the page slides as the cover flies.
@Stable
class CoverFlight {
    private val seen = HashMap<Long, Rect>()
    private val landed = HashMap<Long, Rect>()
    private var lastOut: Title? = null
    var title by mutableStateOf<Title?>(null)
        private set
    var from by mutableStateOf(Rect.Zero)
        private set
    var to by mutableStateOf<Rect?>(null)
    var back by mutableStateOf(false)
        private set

    // Whether the flying cover has been drawn, so its places stay shown until then and the cover is never missing for a frame.
    var airborne by mutableStateOf(false)

    // Counts the flights, so a flight back with the same title starts its motion over.
    var trip by mutableIntStateOf(0)
        private set
    private var armedAt = 0L

    fun saw(id: Long, bounds: Rect) {
        if (bounds.isEmpty) return
        seen[id] = bounds
        if (back && title?.id == id) to = bounds
    }

    fun forget(id: Long) {
        seen.remove(id)
    }

    // Starts a flight for a cover that was seen on the screen.
    fun launch(next: Title) {
        val start = seen[next.id] ?: return
        // Where the cover landed before is forgotten, so a page that shows no place for it does not send it back from an old one.
        landed.remove(next.id)
        airborne = false
        from = start
        to = null
        back = false
        armedAt = nowMillis()
        lastOut = next
        trip++
        title = next
    }

    fun land(id: Long, bounds: Rect) {
        // A place not laid out yet has no size, and a cover cannot fly to it.
        if (bounds.isEmpty) return
        landed[id] = bounds
        if (!back && title?.id == id && nowMillis() - armedAt < WaitMs) to = bounds
    }

    // Starts the flight back when the page of a title that flew out is closed and its tile is still there.
    fun goBack(id: Long) {
        val out = lastOut?.takeIf { it.id == id } ?: return
        val start = landed[id] ?: return
        val home = seen[id] ?: return
        lastOut = null
        airborne = false
        from = start
        to = home
        back = true
        trip++
        title = out
    }

    // Whether the cover of a title is in the air, so its places stay empty until it lands.
    fun flying(id: Long): Boolean = title?.id == id

    fun done() {
        title = null
        airborne = false
        to = null
        back = false
    }
}

val LocalCoverFlight = staticCompositionLocalOf<CoverFlight?> { null }

// Keeps where the cover of a tile is, so a tap can fly it from there. While a cover flies back to it the tile shows nothing in its place.
@Composable
fun Modifier.coverFlightFrom(titleId: Long): Modifier {
    val flight = LocalCoverFlight.current ?: return this
    DisposableEffect(titleId) { onDispose { flight.forget(titleId) } }
    return onGloballyPositioned { flight.saw(titleId, it.boundsInRoot()) }.graphicsLayer {
        alpha = if (flight.back && flight.flying(titleId)) 0f else 1f
    }
}

// The place of the cover on the page of a title, where a flying cover lands and where it leaves from when the page is closed. The cover itself shows while nothing flies.
@Composable
fun Modifier.coverFlightTo(titleId: Long): Modifier {
    val flight = LocalCoverFlight.current ?: return this
    val leaving = LocalPageLeaving.current
    LaunchedEffect(leaving) { if (leaving) flight.goBack(titleId) }
    return onGloballyPositioned { flight.land(titleId, it.boundsInRoot()) }.graphicsLayer {
        alpha = if (flight.flying(titleId) && flight.airborne) 0f else 1f
    }
}

// Draws the flying cover over everything. The box it is in is measured, since the places are measured on the window.
@Composable
fun CoverFlightLayer(flight: CoverFlight, modifier: Modifier = Modifier) {
    val title = flight.title ?: return
    val trip = flight.trip
    val motionOn = !LocalReduceMotion.current
    val progress = remember(trip) { Animatable(0f) }
    var corner by remember { mutableStateOf(Offset.Zero) }
    var room by remember { mutableStateOf(Size.Zero) }
    LaunchedEffect(trip) {
        // A page that never shows its cover lets the flight go.
        delay(WaitMs)
        if (flight.to == null) flight.done()
    }
    val to = flight.to
    LaunchedEffect(trip, to != null) {
        if (to == null) return@LaunchedEffect
        if (motionOn) progress.animateTo(1f, tween(if (flight.back) BackMs else OutMs, easing = Glide))
        flight.done()
    }
    val density = LocalDensity.current
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            corner = it.positionInRoot()
            room = Size(it.size.width.toFloat(), it.size.height.toFloat())
        },
    ) {
        val target = to ?: return@Box
        val start = flight.from
        // A cover going back to a tile that is off the screen is let go at once.
        if (flight.back && room != Size.Zero && !Rect(corner, room).overlaps(target)) {
            LaunchedEffect(trip) { flight.done() }
            return@Box
        }
        val at = progress.value
        val left = start.left + (target.left - start.left) * at - corner.x
        val top = start.top + (target.top - start.top) * at - corner.y
        val width = start.width + (target.width - start.width) * at
        val height = start.height + (target.height - start.height) * at
        // A cover with no size has nothing to draw, and a ratio of nothing would stop the app.
        if (width < 1f || height < 1f) return@Box
        // The cover lifts off the page in the middle of the flight and settles flat where it lands.
        val arc = sin(at * PI).toFloat()
        SideEffect { flight.airborne = true }
        with(density) {
            CoverArt(
                title,
                Modifier
                    .graphicsLayer {
                        translationX = left
                        translationY = top
                        scaleX = 1f + 0.05f * arc
                        scaleY = 1f + 0.05f * arc
                    }.shadow((18 * arc).dp, MaterialTheme.shapes.small)
                    .size(width.toDp(), height.toDp()),
                letterSize = 24,
                aspect = width / height,
            )
        }
    }
}
