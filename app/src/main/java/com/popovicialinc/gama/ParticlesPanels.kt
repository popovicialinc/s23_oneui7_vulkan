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
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt



@Composable
fun ParticlesPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    particlesEnabled: Boolean,
    onParticlesChange: (Boolean) -> Unit,
    // ── style selector ────────────────────────────────────────────────────────
    matrixMode: Boolean,
    onMatrixModeChange: (Boolean) -> Unit,
    onParticlesSettingsClick: () -> Unit,
    onMatrixSettingsClick: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(
            text = LocalStrings.current["particles.title"].ifEmpty { "PARTICLES" },
            fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors = colors
        )

        // ── Master enable toggle ───────────────────────────────────────────────
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 5) {
            ToggleCard(
                title       = LocalStrings.current["particles.toggle"].ifEmpty { "PARTICLES" },
                description = LocalStrings.current["particles.toggle_desc"].ifEmpty {
                    "Animates the background with floating particles or Matrix rain"
                },
                checked         = particlesEnabled,
                onCheckedChange = { performHaptic(); onParticlesChange(it) },
                colors          = colors, cardBackground = cardBackground,
                isSmallScreen   = isSmallScreen, oledMode = oledMode
            )
        }

        // ── Style selector — Stars vs Matrix Rain ─────────────────────────────
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4, enabled = particlesEnabled) {
            val styleCardScale by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = MotionTokens.Springs.gentle.dampingRatio,
                    stiffness = MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.gentle.stiffness, LocalAnimationSpeed.current)
                ),
                label = "style_card_scale"
            )
            val styleCardAlpha by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.25f,
                animationSpec = tween(durationMillis = 300, easing = MotionTokens.Easing.velvet),
                label = "style_card_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = styleCardScale, scaleY = styleCardScale, alpha = styleCardAlpha)
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shape = RoundedCornerShape(28.dp)
                            clip = false
                        },
                    colors    = CardDefaults.cardColors(containerColor = cardBackground),
                    shape     = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier  = Modifier.fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text         = LocalStrings.current["particles.style"].ifEmpty { "STYLE" },
                            fontSize     = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color        = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        Text(
                            text         = if (matrixMode)
                                LocalStrings.current["particles.style_matrix_desc"].ifEmpty { "Cascading columns of glyphs — the classic Matrix digital rain effect" }
                            else
                                LocalStrings.current["particles.style_particles_desc"].ifEmpty { "Twinkling stars that float and shift with device tilt via parallax" },
                            fontSize     = ts.bodyMedium, color = colors.textSecondary,
                            fontFamily   = quicksandFontFamily, fontWeight = FontWeight.Bold
                        )
                        GlideOptionSelector(
                            options         = listOf(
                                LocalStrings.current["particles.style_particles"].ifEmpty { "Stars" },
                                LocalStrings.current["particles.style_matrix"].ifEmpty { "Matrix" }
                            ),
                            selectedIndex   = if (matrixMode) 1 else 0,
                            onOptionSelected = { idx ->
                                performHaptic()
                                if (particlesEnabled) onMatrixModeChange(idx == 1)
                            },
                            colors  = colors,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = particlesEnabled
                        )
                    }
                }
            }
        }

        // ── PARTICLES SETTINGS nav card ───────────────────────────────────────
        // Active when style = Particles; disabled (scaled down + dimmed) when Matrix
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4, enabled = particlesEnabled && !matrixMode) {
            val particlesSettingsEnabled = particlesEnabled && !matrixMode
            SettingsNavigationCard(
                title       = LocalStrings.current["particles.particles_settings_title"].ifEmpty { "PARTICLE SETTINGS" },
                description = LocalStrings.current["particles.particles_settings_desc"].ifEmpty {
                    "Shape, star mode, time mode, speed, parallax, and count"
                },
                onClick       = { if (particlesSettingsEnabled) { performHaptic(); onParticlesSettingsClick() } },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode,
                enabled       = particlesSettingsEnabled
            )
        }

        // ── MATRIX SETTINGS nav card ──────────────────────────────────────────
        // Active when style = Matrix; disabled (scaled down + dimmed) when Particles
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 4, enabled = particlesEnabled && matrixMode) {
            val matrixSettingsEnabled = particlesEnabled && matrixMode
            SettingsNavigationCard(
                title       = LocalStrings.current["particles.matrix_settings_title"].ifEmpty { "MATRIX SETTINGS" },
                description = LocalStrings.current["particles.matrix_settings_desc"].ifEmpty {
                    "Glyph colors, fall speed, column density, font size, and trail length"
                },
                onClick       = { if (matrixSettingsEnabled) { performHaptic(); onMatrixSettingsClick() } },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode,
                enabled       = matrixSettingsEnabled
            )
        }
    }
}

@Composable
fun ParticlesSettingsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    particlesEnabled: Boolean,
    onAppearanceClick: () -> Unit,
    onMotionClick: () -> Unit,
    onPerformanceClick: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(
            text     = LocalStrings.current["particles.particles_settings_title"].ifEmpty { "PARTICLE\nSETTINGS" },
            fontSize = if (isLandscape) ts.displaySmall else ts.displayMedium,
            colors   = colors
        )

        val navAlpha by animateFloatAsState(
            targetValue   = if (particlesEnabled) 1f else 0.38f,
            animationSpec = tween(durationMillis = 260, easing = MotionTokens.Easing.velvet),
            label         = "ps_nav_alpha"
        )

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4, enabled = particlesEnabled) {
            Box(modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = navAlpha)) {
                SettingsNavigationCard(
                    title       = LocalStrings.current["particles.appearance_title"].ifEmpty { "SHAPE & LOOK" },
                    description = LocalStrings.current["particles.appearance_desc"].ifEmpty {
                        "Star mode, time-of-day sky, and visual style"
                    },
                    onClick       = { if (particlesEnabled) { performHaptic(); onAppearanceClick() } },
                    isSmallScreen = isSmallScreen, colors = colors,
                    cardBackground = cardBackground, oledMode = oledMode,
                    enabled       = particlesEnabled
                )
            }
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4, enabled = particlesEnabled) {
            Box(modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = navAlpha)) {
                SettingsNavigationCard(
                    title       = LocalStrings.current["particles.motion_title"].ifEmpty { "MOTION" },
                    description = LocalStrings.current["particles.motion_desc"].ifEmpty {
                        "Float speed, parallax tilt, and sensitivity"
                    },
                    onClick       = { if (particlesEnabled) { performHaptic(); onMotionClick() } },
                    isSmallScreen = isSmallScreen, colors = colors,
                    cardBackground = cardBackground, oledMode = oledMode,
                    enabled       = particlesEnabled
                )
            }
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4, enabled = particlesEnabled) {
            Box(modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = navAlpha)) {
                SettingsNavigationCard(
                    title       = LocalStrings.current["particles.performance_title"].ifEmpty { "PERFORMANCE" },
                    description = LocalStrings.current["particles.performance_desc"].ifEmpty {
                        "Particle count and render refresh rate"
                    },
                    onClick       = { if (particlesEnabled) { performHaptic(); onPerformanceClick() } },
                    isSmallScreen = isSmallScreen, colors = colors,
                    cardBackground = cardBackground, oledMode = oledMode,
                    enabled       = particlesEnabled
                )
            }
        }
    }
}

@Composable
fun ParticlesAppearancePanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    particlesEnabled: Boolean,
    particleStarMode: Boolean,
    onParticleStarModeChange: (Boolean) -> Unit,
    particleTimeMode: Boolean,
    onParticleTimeModeChange: (Boolean) -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(text = LocalStrings.current["particles.appearance_title"].ifEmpty { "SHAPE &\nLOOK" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 2, enabled = particlesEnabled) {
            ToggleCard(
                title = LocalStrings.current["particles.time_mode"].ifEmpty { "TIME MODE" },
                description = LocalStrings.current["particles.time_mode_desc"].ifEmpty { "A sun and moon travel across the sky in sync with the real time of day" },
                checked = particleTimeMode,
                onCheckedChange = { performHaptic(); onParticleTimeModeChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                enabled = particlesEnabled
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 2, enabled = particlesEnabled) {
            ToggleCard(
                title = LocalStrings.current["particles.star_mode"].ifEmpty { "STAR MODE" },
                description = LocalStrings.current["particles.star_mode_desc"].ifEmpty { "Replaces floating dots with tiny twinkling stars — pairs well with Time Mode at night" },
                checked = particleStarMode,
                onCheckedChange = { performHaptic(); onParticleStarModeChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                enabled = particlesEnabled && !particleTimeMode
            )
        }
    }
}

@Composable
fun ParticlesMotionPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    particlesEnabled: Boolean,
    particleSpeed: Int,
    onParticleSpeedChange: (Int) -> Unit,
    particleParallaxEnabled: Boolean,
    onParticleParallaxChange: (Boolean) -> Unit,
    particleParallaxSensitivity: Int,
    onParticleParallaxSensitivityChange: (Int) -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(text = LocalStrings.current["particles.motion_title"].ifEmpty { "MOTION" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)

        // Speed card
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4, enabled = particlesEnabled) {
            val cardScale by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = MotionTokens.Springs.gentle.dampingRatio,
                    stiffness = MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.gentle.stiffness, LocalAnimationSpeed.current)
                ),
                label = "speed_scale"
            )
            val cardAlpha by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.25f,
                animationSpec = tween(durationMillis = 260, easing = MotionTokens.Easing.velvet),
                label = "speed_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = cardScale, scaleY = cardScale, alpha = cardAlpha)
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shape = RoundedCornerShape(28.dp)
                            clip = false
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = LocalStrings.current["particles.speed"].ifEmpty { "SPEED" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        Text(
                            text = LocalStrings.current["particles.speed_desc"].ifEmpty { "Slow is 1× · Medium is 3× · Fast is 6×" },
                            fontSize = ts.bodySmall, color = colors.textSecondary,
                            fontFamily = quicksandFontFamily, fontWeight = FontWeight.Bold
                        )
                        GlideOptionSelector(
                            options = listOf(tr("particles.speed_slow", "Slow"), tr("particles.speed_medium", "Medium"), tr("particles.speed_fast", "Fast")),
                            selectedIndex = particleSpeed.coerceIn(0, 2),
                            onOptionSelected = { performHaptic(); onParticleSpeedChange(it) },
                            colors = colors, modifier = Modifier.fillMaxWidth(),
                            enabled = particlesEnabled
                        )
                    }
                }
            }
        }

        // Parallax toggle
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4, enabled = particlesEnabled) {
            ToggleCard(
                title = LocalStrings.current["particles.parallax"].ifEmpty { "PARALLAX" },
                description = LocalStrings.current["particles.parallax_desc"].ifEmpty { "Tilt your device and the particles shift with it for a subtle 3D depth effect" },
                checked = particleParallaxEnabled,
                onCheckedChange = { performHaptic(); onParticleParallaxChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                enabled = particlesEnabled
            )
        }

        // Parallax sensitivity — only meaningful when parallax is enabled
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4, enabled = particlesEnabled && particleParallaxEnabled) {
            val sensEnabled = particlesEnabled && particleParallaxEnabled
            val sensAlpha by animateFloatAsState(
                targetValue = if (sensEnabled) 1f else 0.38f,
                animationSpec = tween(durationMillis = 260, easing = MotionTokens.Easing.velvet),
                label = "sens_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = sensAlpha)
                    .then(
                        if (sensEnabled) Modifier.border(
                            width = 1.dp,
                            color = colors.primaryAccent.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(28.dp)) else Modifier
                    )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shape = RoundedCornerShape(28.dp)
                            clip = false
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = LocalStrings.current["particles.parallax_sensitivity"].ifEmpty { "PARALLAX SENSITIVITY" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        GlideOptionSelector(
                            options = listOf(tr("particles.sensitivity_low", "Low"), tr("particles.sensitivity_medium", "Medium"), tr("particles.sensitivity_high", "High")),
                            selectedIndex = particleParallaxSensitivity.coerceIn(0, 2),
                            onOptionSelected = { performHaptic(); onParticleParallaxSensitivityChange(it) },
                            colors = colors, modifier = Modifier.fillMaxWidth(),
                            enabled = sensEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParticlesPerformancePanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    particlesEnabled: Boolean,
    particleCount: Int,
    onParticleCountChange: (Int) -> Unit,
    nativeRefreshRate: Boolean,
    onNativeRefreshRateChange: (Boolean) -> Unit,
    quarterRefreshRate: Boolean,
    onQuarterRefreshRateChange: (Boolean) -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(text = LocalStrings.current["particles.performance_title"].ifEmpty { "PERFORMANCE" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)

        // Count card
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4, enabled = particlesEnabled) {
            val cardScale by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = MotionTokens.Springs.gentle.dampingRatio,
                    stiffness = MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.gentle.stiffness, LocalAnimationSpeed.current)
                ),
                label = "count_scale"
            )
            val cardAlpha by animateFloatAsState(
                targetValue = if (particlesEnabled) 1f else 0.25f,
                animationSpec = tween(durationMillis = 260, easing = MotionTokens.Easing.velvet),
                label = "count_alpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = cardScale, scaleY = cardScale, alpha = cardAlpha)
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shape = RoundedCornerShape(28.dp)
                            clip = false
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = LocalStrings.current["particles.count"].ifEmpty { "COUNT" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        Text(
                            text = LocalStrings.current["particles.count_desc"].ifEmpty { "Low = 75 · Medium = 150 · High = 300" },
                            fontSize = ts.bodySmall, color = colors.textSecondary,
                            fontFamily = quicksandFontFamily, fontWeight = FontWeight.Bold
                        )
                        GlideOptionSelector(
                            options = listOf(tr("particles.sensitivity_low", "Low"), tr("particles.sensitivity_medium", "Medium"), tr("particles.sensitivity_high", "High")),
                            selectedIndex = particleCount.coerceIn(0, 2),
                            onOptionSelected = { performHaptic(); onParticleCountChange(it) },
                            colors = colors, modifier = Modifier.fillMaxWidth(),
                            enabled = particlesEnabled
                        )
                    }
                }
            }
        }

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 2, enabled = particlesEnabled) {
            val refreshOption = when {
                nativeRefreshRate -> 0
                quarterRefreshRate -> 2
                else -> 1
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = LocalStrings.current["text_catalog.particle_refresh_rate"].ifEmpty { "PARTICLE REFRESH RATE" },
                        fontSize = ts.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = quicksandFontFamily,
                        color = colors.primaryAccent.copy(alpha = 0.7f)
                    )
                    Text(
                        text = LocalStrings.current["text_catalog.controls_how_often_particles_update_and_how_often_the_rotati"].ifEmpty { "Controls how often particles update and how often the rotation sensor is polled. Native = every frame, 1/2 = every other frame, 1/4 = every fourth frame." },
                        fontSize = ts.bodySmall,
                        color = colors.textSecondary,
                        fontFamily = quicksandFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    GlideOptionSelector(
                        options = listOf(tr("text_catalog.native", "Native"), "1/2", "1/4"),
                        selectedIndex = refreshOption,
                        onOptionSelected = { option ->
                            performHaptic()
                            when (option) {
                                0 -> {
                                    onNativeRefreshRateChange(true)
                                    onQuarterRefreshRateChange(false)
                                }
                                1 -> {
                                    onNativeRefreshRateChange(false)
                                    onQuarterRefreshRateChange(false)
                                }
                                else -> {
                                    onNativeRefreshRateChange(false)
                                    onQuarterRefreshRateChange(true)
                                }
                            }
                        },
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = particlesEnabled
                    )
                }
            }
        }
    }
}

