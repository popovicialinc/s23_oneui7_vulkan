package com.popovicialinc.gama


import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt



@Composable
internal fun tr(key: String, fallback: String): String =
    LocalStrings.current[key].ifEmpty { fallback }

/**
 * Keeps settings cards readable in portrait while using the unused horizontal
 * space in landscape. Each card is measured within one equal-width column, so
 * text reflows instead of shrinking or leaving a single oversized row.
 */
@Composable
internal fun ResponsiveSettingsCardGrid(
    isLandscape: Boolean,
    cards: List<@Composable () -> Unit>
) {
    if (!isLandscape) {
        cards.forEach { it() }
        return
    }

    CompositionLocalProvider(LocalLandscapePanelGrid provides false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowCards.forEach { card ->
                    Box(modifier = Modifier.weight(1f)) { card() }
                }
                if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    }
}

@Composable
internal fun PanelScaffold(
    visible: Boolean,
    onDismiss: () -> Unit,
    isLandscape: Boolean,
    isSmallScreen: Boolean,
    isBlurred: Boolean = false,
    oledMode: Boolean = false,
    rootExitCascade: Boolean = false,
    colors: ThemeColors,
    leadingFloatingButton: (@Composable (Modifier, FloatingButtonHoldState, Boolean) -> Unit)? = null,
    edgeSpacers: Boolean = true,
    reserveBackButtonSpace: Boolean = true,
    contentAvoidsBackButton: Boolean = true,
    contentScrollable: Boolean = true,
    content: @Composable ColumnScope.(scrollState: ScrollState) -> Unit
) {
    val dismissOnClickOutside = LocalDismissOnClickOutside.current
    val scrollState = rememberScrollState()
    LaunchedEffect(visible) { if (visible) scrollState.scrollTo(0) }
    val animLevel = LocalAnimationLevel.current
    val animSpeed = LocalAnimationSpeed.current
    val backButtonInversed = LocalBackButtonInversed.current
    val floatingButtonAnchors = LocalFloatingButtonAnchors.current

    // Floating panel chrome should use the same visual language as the cards:
    // fade + slight zoom + tiny vertical lift.
    // Do NOT launch it from far below with an overshooting spring, because that
    // makes the magnifier / globe / < button feel like a different animation system.
    val floatingChromeOffsetYPx = with(LocalDensity.current) {
        when (animLevel) {
            0 -> 20.dp.toPx()
            1 -> 8.dp.toPx()
            else -> 0.dp.toPx()
        }
    }

    // Static bottom padding — reserves space for the floating back button so the
    // last card is never obscured.  Previously this was an animated spring, but
    // animating a .padding() value triggers a full layout pass every frame during
    // the ~300 ms panel-enter transition, causing the visible stutter/freeze.
    // The back button already animates in via graphicsLayer translationY (draw-only,
    // zero layout cost), so the padding just needs to be the correct resting size
    // from the first frame — no animation required.
    val bottomPaddingDp = if (isSmallScreen) 120f else 132f  // dp: button height + gap + clearance
    // When a sub-dialog opens (isBlurred = true) the panel dims so the dialog feels
    // on a higher layer. The blur itself snaps on/off (no animated radius — see below).
    //
    // panelAlpha  — 1→0.35 dims the whole panel as the sub-dialog arrives
    val panelAlpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.38f else 1f,
        animationSpec = if (animLevel == 2) snap() else tween(
            durationMillis = MotionTokens.SpeedUtil.durationMs(if (isBlurred) 300 else 190, animSpeed),
            easing = if (isBlurred) MotionTokens.Easing.emphasizedDecelerate else MotionTokens.Easing.enter
        ),
        label = "panel_dim_alpha"
    )

    BouncyDialog(
        visible = visible,
        onDismiss = onDismiss,
        fullScreen = true,
        exitStartDelayMillis = if (rootExitCascade) {
            when (animLevel) {
                0 -> 105
                1 -> 70
                else -> 0
            }
        } else 0
    ) {
        CompositionLocalProvider(LocalRootPanelExitCascade provides rootExitCascade) {
        // Single render of panel content — no double composition.
        //
        // Previously: panelContent() was called twice (sharp copy + blurred copy),
        // composing the entire panel tree twice during the ~320ms transition.
        //
        // Now: one render, always.  When a sub-dialog opens:
        //   • API 31+: Modifier.blur() is applied in the draw phase only (zero
        //     recomposition overhead) and the panel dims to 35% alpha.
        //   • API < 31: blur is unavailable, the panel just dims to 35% — still
        //     communicates depth without any GPU blur cost.
        //
        // The visual difference on API 31+ is imperceptible: the user is looking
        // at a dialog, not scrutinising the blurred panel behind it.
        val useBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        // Snap blur radius — never animate it per-frame.
        // Previously `(blurAlpha * 20f).dp` caused a new RenderEffect (Gaussian kernel
        // rebuild) every vsync during the ~300 ms transition: ~18 kernel rebuilds back-to-back.
        // Fixed radius set once when isBlurred becomes true → zero per-frame GPU rebuild cost.
        // The panelAlpha fade still provides smooth visual feedback of the transition.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = panelAlpha }
                .then(
                    if (useBlur && isBlurred)
                        Modifier.blur(
                            radius = 20.dp,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                    else Modifier
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background tap layer. It sits behind the cards/back button, so it
                // keeps outside-tap dismissal without stealing child clicks.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (visible) Modifier.pointerInput(dismissOnClickOutside) {
                                if (dismissOnClickOutside) detectTapGestures { onDismiss() }
                                else detectTapGestures { }
                            } else Modifier
                        )
                )

                val backButtonSize = if (isSmallScreen) 48.dp else 52.dp
                // Landscape keeps a symmetric safe gutter on both sides. This
                // preserves the concentric card frame and prevents either floating
                // control (back, search, or global) from covering card content.
                val horizontalPadding = if (isLandscape) backButtonSize + 48.dp else 24.dp

                CompositionLocalProvider(
                    LocalFloatingBackButtonAvoidance provides FloatingBackButtonAvoidance(
                        enabled = visible && contentAvoidsBackButton && LocalBackButtonAvoidanceEnabled.current,
                        endPadding = backButtonSize + 32.dp,
                        bottomPadding = if (isSmallScreen) 44.dp else 52.dp,
                        buttonSize = backButtonSize
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = if (isLandscape) 800.dp else 500.dp)
                            .then(
                                if (contentScrollable) Modifier.verticalScroll(scrollState)
                                else Modifier.fillMaxHeight()
                            )
                            .padding(horizontal = horizontalPadding)
                            .padding(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                bottom = if (reserveBackButtonSpace) bottomPaddingDp.dp else 0.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 16.dp else 20.dp)
                    ) {
                        if (edgeSpacers) Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 40.dp))
                        CompositionLocalProvider(LocalPanelScrollState provides scrollState) {
                            content(scrollState)
                        }
                        if (edgeSpacers) Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                var floatingChromeVisible by remember { mutableStateOf(visible) }
                LaunchedEffect(visible, animLevel) {
                    if (visible) {
                        floatingChromeVisible = true
                    } else {
                        // Root panels keep a short foreground-first beat. Deeper panels
                        // should leave immediately so sub-menu navigation stays fast.
                        if (animLevel != 2 && rootExitCascade) delay(85L)
                        floatingChromeVisible = false
                    }
                }

                val floatingChromeProgress by animateFloatAsState(
                    targetValue = if (floatingChromeVisible) 1f else 0f,
                    animationSpec = when (animLevel) {
                        2 -> snap()
                1 -> tween(durationMillis = MotionTokens.SpeedUtil.durationMs(120, animSpeed), easing = MotionTokens.Easing.emphasizedDecelerate)
                else -> spring(dampingRatio = 0.70f, stiffness = MotionTokens.SpeedUtil.stiffness(520f, animSpeed))
                    },
                    label = "panel_floating_chrome_progress"
                )
                val floatingChromeScale = 0.94f + floatingChromeProgress * 0.06f
                val floatingChromeTranslationY = (1f - floatingChromeProgress) * floatingChromeOffsetYPx

                fun anchorModifier(isLeft: Boolean): Modifier {
                    // PanelBackButton and the leading controls measure to their glow,
                    // not their visible 52dp circle. Centre that shared footprint on
                    // the saved anchor so every control truly lands at the same point.
                    val glowSize = backButtonSize * 1.8f
                    val x = if (floatingButtonAnchors.fullWidth) {
                        maxWidth * if (isLeft) floatingButtonAnchors.leftX else floatingButtonAnchors.rightX
                    } else if (isLeft) {
                        maxWidth * 0.5f * floatingButtonAnchors.leftX
                    } else {
                        maxWidth * (0.5f + 0.5f * floatingButtonAnchors.rightX)
                    }
                    val y = maxHeight * if (isLeft) floatingButtonAnchors.leftY else floatingButtonAnchors.rightY
                    return Modifier
                        .align(Alignment.TopStart)
                        .offset(x = x - glowSize / 2, y = y - glowSize / 2)
                }

                val leadingHoldState = remember { FloatingButtonHoldState() }
                val backHoldState = remember { FloatingButtonHoldState() }

                leadingFloatingButton?.invoke(
                    Modifier
                        .then(anchorModifier(!backButtonInversed))
                        .graphicsLayer(
                            alpha = floatingChromeProgress,
                            scaleX = floatingChromeScale,
                            scaleY = floatingChromeScale,
                            translationX = leadingHoldState.dragTranslationX,
                            translationY = floatingChromeTranslationY + leadingHoldState.dragTranslationY
                        ),
                    leadingHoldState,
                    !backButtonInversed
                )

                PanelBackButton(
                    onClick = onDismiss,
                    colors = colors,
                    oledMode = oledMode,
                    isSmallScreen = isSmallScreen,
                    enabled = visible,
                    scrollState = scrollState,
                    floatingHoldState = backHoldState,
                    isLeftSide = backButtonInversed,
                    modifier = Modifier
                        .then(anchorModifier(backButtonInversed))
                        .graphicsLayer(
                            alpha = floatingChromeProgress,
                            scaleX = floatingChromeScale,
                            scaleY = floatingChromeScale,
                            translationX = backHoldState.dragTranslationX,
                            translationY = floatingChromeTranslationY + backHoldState.dragTranslationY
                        )
                )
            }
        }
        }
    }
}

@Composable
internal fun PanelCaption(
    text: String,
    colors: ThemeColors,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val ts = LocalTypeScale.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.background.copy(alpha = 0.18f),
                        colors.background.copy(alpha = 0.26f),
                        colors.background.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 30.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = ts.bodySmall,
            color = if (accent) colors.primaryAccent.copy(alpha = 0.70f)
                    else colors.textPrimary.copy(alpha = 0.62f),
            fontFamily = quicksandFontFamily,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 19.sp
        )
    }
}

