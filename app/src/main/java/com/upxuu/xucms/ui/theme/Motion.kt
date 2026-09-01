package com.upxuu.xucms.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * One place for every duration and curve in the app.
 *
 * Springs are preferred over fixed durations for anything the user's finger or
 * caret drives: a spring keeps velocity continuous, so a second gesture arriving
 * mid-animation blends instead of restarting. Tweens are reserved for discrete
 * state flips (a colour swapping, a panel appearing) where there is no velocity to
 * preserve.
 */
object Motion {

  /** Material's standard easing; slower out than in, so motion settles rather than stops. */
  val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
  val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
  val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

  const val QUICK = 140
  const val NORMAL = 240
  const val SLOW = 340

  /** For a value that should feel physical: scroll offsets, drag settle, scale. */
  fun <T> gentleSpring(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.9f, stiffness = 300f)

  /** Slightly livelier, for small affordances like a selected icon growing. */
  fun <T> snappySpring(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

  /** Offsets need their own spec because IntOffset has no default visibility threshold. */
  fun offsetSpring(): FiniteAnimationSpec<IntOffset> =
    spring(dampingRatio = 0.9f, stiffness = 300f, visibilityThreshold = IntOffset(1, 1))

  fun <T> quickTween(): FiniteAnimationSpec<T> = tween(QUICK, easing = Standard)

  fun <T> normalTween(): FiniteAnimationSpec<T> = tween(NORMAL, easing = Standard)

  fun <T> enterTween(): FiniteAnimationSpec<T> = tween(NORMAL, easing = Decelerate)

  fun <T> exitTween(): FiniteAnimationSpec<T> = tween(QUICK, easing = Accelerate)
}
