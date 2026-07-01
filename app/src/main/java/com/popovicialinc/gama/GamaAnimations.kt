package com.popovicialinc.gama


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.Rect as AndroidRect
import android.view.HapticFeedbackConstants
import android.view.PixelCopy
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt


data class FloatingBackButtonAvoidance(
    val enabled: Boolean = false,
    val endPadding: Dp = 0.dp,
    val bottomPadding: Dp = 0.dp,
    val buttonSize: Dp = 0.dp
)

val LocalFloatingBackButtonAvoidance = compositionLocalOf { FloatingBackButtonAvoidance() }

internal val LocalStaggerCounter = compositionLocalOf<StaggerCounter?> { null }

val LocalRootPanelExitCascade = compositionLocalOf { false }

class StaggerCounter {
    private var index = 0
    private var total = 0
    fun next(): Int = index++
    fun reset() {
        index = 0
    }

    fun setTotal(n: Int) {
        total = n
    }

    fun total(): Int = total
}

@Composable
fun StaggerScope(
    totalItems: Int = 0,   // optional override; if 0, auto-counted
    content: @Composable () -> Unit
) {
    val counter = remember { StaggerCounter() }
    // Reset index at the start of every composition so indices don't
    // accumulate across recompositions.
    counter.reset()
    if (totalItems > 0) counter.setTotal(totalItems)
    CompositionLocalProvider(LocalStaggerCounter provides counter) {
        content()
    }
}

@Composable
fun AnimatedElement(
    visible: Boolean,
    staggerIndex: Int = -1,   // -1 = auto-assign from StaggerScope
    totalItems: Int = 0,
    modifier: Modifier = Modifier,
    cardShadow: Boolean = false,  // true = draw directional shadow (cards only, not buttons/text)
    enabled: Boolean = true,      // false = suppress shadow with ease-in-out animation
    avoidBackButton: Boolean = false, // true = use the floating < avoidance layout without drawing wrapper shadow
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val staggerCounter = LocalStaggerCounter.current
    val resolvedIndex = if (staggerIndex >= 0) staggerIndex
    else staggerCounter?.next() ?: 0
    val resolvedTotal = if (totalItems > 0) totalItems
    else staggerCounter?.total() ?: 0
    val animationLevel = LocalAnimationLevel.current
    val animSpeed = LocalAnimationSpeed.current
    val staggerEnabled = LocalStaggerEnabled.current
    val rootPanelExitCascade = LocalRootPanelExitCascade.current
    val view = LocalView.current
    val context = LocalContext.current
    val backButtonAvoidance = LocalFloatingBackButtonAvoidance.current
    val backButtonInversed = LocalBackButtonInversed.current
    var overlapsFloatingBackButton by remember { mutableStateOf(false) }
    var lastAvoidanceHapticAtMs by remember { mutableStateOf(0L) }
    val shouldAvoidBackButton = cardShadow || avoidBackButton

    val avoidEndPadding by animateDpAsState(
        targetValue = if (shouldAvoidBackButton && backButtonAvoidance.enabled && overlapsFloatingBackButton)
            backButtonAvoidance.endPadding
        else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = MotionTokens.SpeedUtil.stiffness(300f, animSpeed)
        ),
        label = "floating_back_button_avoidance"
    )

    // ── Performance-optimised stagger ────────────────────────────────────────
    //
    // Previous implementation: 3 separate animateFloatAsState per card
    // (alpha, scale, translationY) → 21 simultaneous animators for a 7-item
    // panel, all recomposing every frame, causing the frame-drop on open.
    //
    // New implementation: single Animatable<Float> in [0,1] drives all three
    // properties via lerp inside graphicsLayer — ONE animator per card, zero
    // recomposition (graphicsLayer reads are draw-phase only).
    //
    // Stagger gap reduced 95ms → 60ms so the full cascade finishes faster.
    // Duration reduced: 680/460ms → 420/300ms — still feels smooth but doesn't
    // hold the render thread in animation work for nearly a second.
    // keyframes replaced with a single tween + CubicBezier — lighter to evaluate.
    //
    // When staggerEnabled is false, all items animate simultaneously with no
    // cascade delay — faster perceived performance at the cost of the cascade effect.

    var localVisible by remember { mutableStateOf(if (resolvedIndex == 0) visible else !visible) }
    val progress = remember { Animatable(if (localVisible) 1f else 0f) }


    val offsetYPx = remember(density, animationLevel) {
        with(density) {
            when (animationLevel) {
                0 -> 20f; 1 -> 8f; else -> 0f
            }.dp.toPx()
        }
    }

// settled = false while this card is mid-stagger, true once it lands.
// Cards inside read LocalCardSettled to drive their shadow fade-in.
    var settled by remember { mutableStateOf(progress.value >= 0.999f) }

    LaunchedEffect(visible) {
        settled = false
        if (visible) {
            if (resolvedIndex > 0 && animationLevel != 2 && staggerEnabled) {
                delay(
                    (resolvedIndex * 60L * MotionTokens.SpeedUtil.durationMs(100, animSpeed) / 100).toLong()
                        .coerceAtLeast(0L)
                )
            }
            localVisible = true
            if (animationLevel == 2) {
                progress.snapTo(1f)
            } else {
                // Full = springy. Reduced = no bounce, simple ease-in-out.
                val enterSpec: FiniteAnimationSpec<Float> = if (animationLevel == 0)
                    spring(dampingRatio = 0.55f, stiffness = MotionTokens.SpeedUtil.stiffness(260f, animSpeed))
                else
                    tween(
                        durationMillis = MotionTokens.SpeedUtil.durationMs(210, animSpeed),
                        easing = MotionTokens.Easing.emphasizedDecelerate
                    )
                progress.animateTo(targetValue = 1f, animationSpec = enterSpec)
            }
        } else {
            if (resolvedIndex > 0 && animationLevel != 2 && staggerEnabled) {
                if (rootPanelExitCascade) {
                    // Level-1/root panels: cinematic reverse stagger. Top chrome exits
                    // first, lower cards follow, then the panel shell fades.
                    delay(
                        (resolvedIndex * 18L * MotionTokens.SpeedUtil.durationMs(100, animSpeed) / 100).toLong()
                            .coerceAtLeast(0L)
                    )
                } else if (resolvedTotal > 0) {
                    // Deeper sub-panels: old style. Faster and less ceremonial, so
                    // drilling in/out of menus feels immediate.
                    delay(
                        ((resolvedTotal - resolvedIndex) * 22L * MotionTokens.SpeedUtil.durationMs(
                            100,
                            animSpeed
                        ) / 100).toLong().coerceAtLeast(0L)
                    )
                }
            }
            localVisible = false
            if (animationLevel == 2) {
                progress.snapTo(0f)
            } else {
                // Exit: quicker than the previous pass, but still visually readable.
                val exitSpec = tween<Float>(
                    durationMillis = MotionTokens.SpeedUtil.durationMs(
                        if (animationLevel == 0) 120 else 85,
                        animSpeed
                    ),
                    easing = MotionTokens.Easing.exit
                )
                progress.animateTo(
                    targetValue = 0f, animationSpec = exitSpec
                )
            }
        }
        settled = true
    }

    CompositionLocalProvider(
        LocalCardSettled provides settled,
        LocalCardProgress provides progress.value,
        LocalCardEnabled provides enabled
    ) {
        val avoidanceModifier = if (shouldAvoidBackButton && backButtonAvoidance.enabled) {
            Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val itemTop = position.y
                val itemBottom = itemTop + coordinates.size.height
                val screenHeight = view.height.toFloat().takeIf { it > 0f } ?: return@onGloballyPositioned

                val avoidZoneTop = screenHeight - with(density) {
                    (backButtonAvoidance.bottomPadding + backButtonAvoidance.buttonSize + 16.dp).toPx()
                }

                val overlaps = itemBottom > avoidZoneTop && itemTop < screenHeight
                if (overlapsFloatingBackButton != overlaps) {
                    overlapsFloatingBackButton = overlaps
                    val now = SystemClock.uptimeMillis()
                    if (now - lastAvoidanceHapticAtMs > 140L) {
                        lastAvoidanceHapticAtMs = now
                        if (overlaps) {
                            GamaHaptics.avoidanceDodge(context, view)
                        } else {
                            GamaHaptics.avoidanceReturn(context, view)
                        }
                    }
                }
            }
        } else Modifier

        val safeAvoidEndPadding = avoidEndPadding.coerceAtLeast(0.dp)
        val cardModifier = modifier
            .then(avoidanceModifier)
            .padding(
                start = if (backButtonInversed) safeAvoidEndPadding else 0.dp,
                end = if (backButtonInversed) 0.dp else safeAvoidEndPadding
            )

        Box(
            modifier = (if (cardShadow) cardModifier.directionalShadow() else cardModifier)
                .graphicsLayer {
                    val p = progress.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = 0.94f + p * 0.06f
                    scaleY = scaleX
                    translationY = (1f - p) * offsetYPx
                    clip = false
                }
        ) {
            content()
        }
    }
}
