package jr.brian.inindy.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.staticCompositionLocalOf

// Central motion system. Every navigation transition and screen animation
// pulls its timings and curves from here so the app has a single, coherent
// motion personality (iOS-style spatial + fades for context switches).
// No composable outside Motion should reference raw millisecond values.
object Motion {
    object Duration {
        // Micro-interactions (arrow rotations, toggle settles, tab crossfades).
        const val Fast: Int = 120
        // Standard drill-down and modal push durations.
        const val Medium: Int = 250
        // Emphasized enter for large content-swap moments (splash → app,
        // loading → success). Slightly slower so the settle reads as intentional.
        const val Emphasized: Int = 350
        // Fallback duration used everywhere when reduced-motion is enabled.
        const val ReducedFade: Int = 90
    }

    // Standard (Material) easing — decelerating, feels responsive on enter.
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    // Emphasized decelerate — settles harder near the end. For hero enters.
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    // Emphasized accelerate — starts slow, exits fast. For screens leaving.
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // === Enter/Exit builders ===
    // Every builder takes reducedMotion; when true it collapses to a short fade
    // so users with the OS reduced-motion setting on get calm transitions.

    // Drill-down push (right-hand slide-in) for detail screens.
    fun pushEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(Duration.ReducedFade, easing = Standard))
        return slideInHorizontally(
            animationSpec = tween(Duration.Medium, easing = EmphasizedDecelerate),
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(tween(Duration.Medium, easing = Standard))
    }

    // Sibling screen sliding off to the left as the new screen pushes in.
    // Parallax only (quarter width) so the outgoing screen doesn't fully leave
    // before the new one is in — reads as a coupled push, not a swap.
    fun pushExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(Duration.ReducedFade, easing = Standard))
        return slideOutHorizontally(
            animationSpec = tween(Duration.Medium, easing = EmphasizedAccelerate),
            targetOffsetX = { fullWidth -> -fullWidth / 4 }
        ) + fadeOut(tween(Duration.Medium, easing = Standard))
    }

    // Pop-enter: previous screen coming back from its parallax offset.
    fun popEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(Duration.ReducedFade, easing = Standard))
        return slideInHorizontally(
            animationSpec = tween(Duration.Medium, easing = EmphasizedDecelerate),
            initialOffsetX = { fullWidth -> -fullWidth / 4 }
        ) + fadeIn(tween(Duration.Medium, easing = Standard))
    }

    // Pop-exit: current screen sliding off to the right on back navigation.
    fun popExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(Duration.ReducedFade, easing = Standard))
        return slideOutHorizontally(
            animationSpec = tween(Duration.Medium, easing = EmphasizedAccelerate),
            targetOffsetX = { fullWidth -> fullWidth }
        ) + fadeOut(tween(Duration.Medium, easing = Standard))
    }

    // Modal enter: slide up from the bottom edge with a matched fade.
    fun modalEnter(reducedMotion: Boolean = false): EnterTransition {
        if (reducedMotion) return fadeIn(tween(Duration.ReducedFade, easing = Standard))
        return slideInVertically(
            animationSpec = tween(Duration.Emphasized, easing = EmphasizedDecelerate),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(tween(Duration.Medium, easing = Standard))
    }

    // The underlying screen stays put when a modal opens on top of it —
    // just a brief fade so it recedes visually while the modal slides in.
    fun modalExit(reducedMotion: Boolean = false): ExitTransition =
        fadeOut(tween(if (reducedMotion) Duration.ReducedFade else Duration.Fast, easing = Standard))

    // Underlying screen re-appearing when the modal is dismissed.
    fun modalPopEnter(reducedMotion: Boolean = false): EnterTransition =
        fadeIn(tween(if (reducedMotion) Duration.ReducedFade else Duration.Fast, easing = Standard))

    // Modal sliding down off the bottom edge on dismiss.
    fun modalPopExit(reducedMotion: Boolean = false): ExitTransition {
        if (reducedMotion) return fadeOut(tween(Duration.ReducedFade, easing = Standard))
        return slideOutVertically(
            animationSpec = tween(Duration.Emphasized, easing = EmphasizedAccelerate),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(tween(Duration.Medium, easing = Standard))
    }

    // Cross-fade for tab switches and graph swaps — no spatial relationship
    // between source and destination, so we intentionally avoid directional slides.
    fun fadeEnter(reducedMotion: Boolean = false): EnterTransition {
        val d = if (reducedMotion) Duration.ReducedFade else Duration.Medium
        return fadeIn(tween(d, easing = Standard))
    }

    fun fadeExit(reducedMotion: Boolean = false): ExitTransition {
        val d = if (reducedMotion) Duration.ReducedFade else Duration.Medium
        return fadeOut(tween(d, easing = Standard))
    }
}

// Provided at the app root from rememberSystemReducedMotion(). When true, every
// Motion builder above collapses to a quick fade. Read via LocalReducedMotion.current
// inside any composable that needs to hand a transition to nav-compose or
// AnimatedContent — capture the value once outside the transition lambda since
// nav lambdas are not @Composable.
val LocalReducedMotion = staticCompositionLocalOf { false }
