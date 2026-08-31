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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt



@Composable
internal fun HapticPreviewButton(
    text: String,
    onClick: () -> Unit,
    colors: ThemeColors,
    oledMode: Boolean,
    modifier: Modifier = Modifier,
    onPress: (() -> Long)? = null,
    onRelease: ((startedAtMs: Long, released: Boolean) -> Unit)? = null,
    enabled: Boolean = true
) {
    val ts = LocalTypeScale.current
    val context = LocalContext.current
    val view = LocalView.current
    val animLevel = LocalAnimationLevel.current
    val animSpeed = LocalAnimationSpeed.current
    val shape = RoundedCornerShape(16.dp)
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = if (animLevel == 2) snap() else spring(
            dampingRatio = MotionTokens.Springs.pressDown.dampingRatio,
            stiffness = MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.pressDown.stiffness, animSpeed)
        ),
        label = "haptic_preview_press"
    )
    val scale = 1f - pressProgress * (1f - MotionTokens.Scale.subtle)
    val borderWidth = (1f + pressProgress * 1f).dp
    val borderAlpha = 0.55f + pressProgress * 0.45f

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(if (!enabled) Modifier.graphicsLayer(alpha = 0.25f, scaleX = 0.85f, scaleY = 0.85f) else Modifier)
            .clip(shape)
            .background(if (oledMode) Color.Black else colors.cardBackground)
            .border(borderWidth, colors.primaryAccent.copy(alpha = borderAlpha), shape)
            .then(if (!enabled) Modifier.pointerInput(enabled) { detectTapGestures { } } else Modifier)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val startedAt = onPress?.invoke() ?: SystemClock.uptimeMillis()
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (onRelease != null) {
                            onRelease(startedAt, released)
                        } else if (released) {
                            // Preview buttons play exactly the pattern selected
                            // by the caller; do not inject an extra generic
                            // contact/hold haptic before or after it.
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.primaryAccent,
            fontSize = ts.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = quicksandFontFamily,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
internal fun HapticStrengthCard(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    preview: () -> Unit,
    previewAtValue: ((Int) -> Unit)? = null,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean,
    isSmallScreen: Boolean,
    available: Boolean = true
) {
    val ts = LocalTypeScale.current
    var lastSliderPreviewAtMs by remember { mutableStateOf(0L) }
    DisabledCardWrapper(enabled = available) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!available) Modifier.pointerInput(available) { detectTapGestures { } } else Modifier)
            .border(1.dp, colors.primaryAccent.copy(alpha = 0.55f), RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 18.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.primaryAccent.copy(alpha = 0.85f),
                        fontSize = ts.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quicksandFontFamily,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = colors.textSecondary,
                        fontSize = ts.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quicksandFontFamily
                    )
                }
                Text(
                    text = "$value%",
                    color = colors.textPrimary,
                    fontSize = ts.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.primaryAccent.copy(alpha = 0.10f))
                        .border(1.dp, colors.primaryAccent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primaryAccent.copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (enabled) LocalStrings.current["text_catalog.pattern_enabled"].ifEmpty { "PATTERN ENABLED" } else LocalStrings.current["text_catalog.pattern_off"].ifEmpty { "PATTERN OFF" },
                    color = if (enabled) colors.primaryAccent else colors.textSecondary,
                    fontSize = ts.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily,
                    letterSpacing = 1.2.sp
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { if (available) onEnabledChange(it) },
                    enabled = available,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.primaryAccent,
                        checkedTrackColor = colors.primaryAccent.copy(alpha = 0.35f),
                        uncheckedThumbColor = colors.textSecondary.copy(alpha = 0.75f),
                        uncheckedTrackColor = colors.textSecondary.copy(alpha = 0.14f)
                    )
                )
            }

            Slider(
                value = value.toFloat(),
                onValueChange = {
                    if (!available) return@Slider
                    val next = it.roundToInt().coerceIn(0, 100)
                    onValueChange(next)
                    val now = SystemClock.uptimeMillis()
                    if (now - lastSliderPreviewAtMs > 90L) {
                        lastSliderPreviewAtMs = now
                        previewAtValue?.invoke(next)
                    }
                },
                onValueChangeFinished = { previewAtValue?.invoke(value) ?: preview() },
                valueRange = 0f..100f,
                steps = 9,
                enabled = available,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primaryAccent,
                    activeTrackColor = colors.primaryAccent,
                    inactiveTrackColor = colors.primaryAccent.copy(alpha = 0.18f),
                    activeTickColor = colors.primaryAccent.copy(alpha = 0.55f),
                    inactiveTickColor = colors.primaryAccent.copy(alpha = 0.20f)
                )
            )

            HapticPreviewButton(
                text = LocalStrings.current["text_catalog.preview_feel"].ifEmpty { "PREVIEW FEEL" },
                onClick = preview,
                colors = colors,
                oledMode = oledMode,
                enabled = available,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    }
}

@Composable
fun HapticsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    regularEnabled: Boolean,
    onRegularEnabledChange: (Boolean) -> Unit,
    holdEnabled: Boolean,
    onHoldEnabledChange: (Boolean) -> Unit,
    rendererEnabled: Boolean,
    onRendererEnabledChange: (Boolean) -> Unit,
    languageEnabled: Boolean,
    onLanguageEnabledChange: (Boolean) -> Unit,
    bounceEnabled: Boolean,
    onBounceEnabledChange: (Boolean) -> Unit,
    bounceReturnEnabled: Boolean,
    onBounceReturnEnabledChange: (Boolean) -> Unit,
    regularStrength: Int,
    onRegularStrengthChange: (Int) -> Unit,
    holdStrength: Int,
    onHoldStrengthChange: (Int) -> Unit,
    rendererStrength: Int,
    onRendererStrengthChange: (Int) -> Unit,
    languageStrength: Int,
    onLanguageStrengthChange: (Int) -> Unit,
    bounceStrength: Int,
    onBounceStrengthChange: (Int) -> Unit,
    bounceReturnStrength: Int,
    onBounceReturnStrengthChange: (Int) -> Unit,
    onResetHaptics: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean,
    performHaptic: () -> Unit
) {
    val ts = LocalTypeScale.current
    val strings = LocalStrings.current
    val context = LocalContext.current
    val view = LocalView.current
    var section by remember { mutableStateOf("overview") }

    fun parentSection(current: String): String = when (current) {
        "dodge", "return" -> "layout"
        else -> "overview"
    }

    BackHandler(enabled = visible && section != "overview") {
        section = parentSection(section)
    }

    val title = when (section) {
        "core" -> strings["haptics.core_title"].ifEmpty { "CORE FEEL" }
        "layout" -> strings["haptics.layout_title"].ifEmpty { "LAYOUT MOTION" }
        "dodge" -> strings["haptics.dodge_left"].ifEmpty { "DODGE LEFT" }
        "return" -> strings["haptics.return_settle"].ifEmpty { "RETURN SETTLE" }
        "reset" -> strings["haptics.reset_title"].ifEmpty { "RESET HAPTICS" }
        else -> strings["haptics.title"].ifEmpty { "HAPTICS" }
    }

    PanelScaffold(
        visible = visible,
        onDismiss = {
            if (section == "overview") onDismiss() else section = parentSection(section)
        },
        isLandscape = isLandscape,
        isSmallScreen = isSmallScreen,
        oledMode = oledMode,
        rootExitCascade = true,
        colors = colors
    ) { scrollState ->
        AnimatedSearchPanelTitle(
            titleKey = "haptics_$section",
            text = title,
            visible = visible,
            fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors = colors,
            scrollState = scrollState
        )

        key("haptics_caption_$section") {
            // Stagger totals below INCLUDE this caption (index 1), matching the
            // settings-hub convention so every section animates caption → cards
            // as one continuous sequence.
            AnimatedElement(visible = visible, staggerIndex = 1, totalItems = when (section) {
                "overview" -> 5
                "core" -> 5
                "layout" -> 5
                "reset" -> 4
                else -> 3   // dodge, return
            }) {
                PanelCaption(
                    text = when (section) {
                "core" -> strings["haptics.core_caption"].ifEmpty { "Everyday tactile language: quick taps, first contact, and the stronger bloom after a deliberate hold." }
                "layout" -> strings["haptics.layout_caption"].ifEmpty { "Mechanical feedback for visual motion. Open each motion pattern separately so the main panel stays clean." }
                "dodge" -> strings["haptics.dodge_left_desc"].ifEmpty { "Gentle tick when a card rescales or moves left to avoid the floating back button." }
                "return" -> strings["haptics.return_settle_desc"].ifEmpty { "Stronger settling pulse when the card returns to normal width. This should feel like the UI snapping home." }
                "reset" -> strings["haptics.reset_caption"].ifEmpty { "Restore the factory GAMA haptics profile if the feel gets messy." }
                        else -> strings["haptics.caption"].ifEmpty { "GAMA has no sound effects by design, so this is the mechanical side of the interface. Tune it like a tiny physical instrument." }
                    },
                    colors = colors
                )
            }
        }

        key(section) {
            when (section) {
            "overview" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
                    ToggleCard(
                        title = strings["haptics.engine"].ifEmpty { "HAPTICS ENGINE" },
                        description = strings["haptics.engine_desc"].ifEmpty { "Master switch for every custom vibration pattern in the app." },
                        checked = hapticsEnabled,
                        onCheckedChange = { performHaptic(); onHapticsEnabledChange(it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        isSmallScreen = isSmallScreen,
                        oledMode = oledMode,
                        accentBorder = true
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
                    SettingsNavigationCard(
                        title = strings["haptics.core_title"].ifEmpty { "CORE FEEL" },
                        description = strings["haptics.core_desc"].ifEmpty { "Regular clicks, first-contact feedback, and press-and-hold release blooms." },
                        onClick = { performHaptic(); section = "core" },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        enabled = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
                    SettingsNavigationCard(
                        title = strings["haptics.layout_title"].ifEmpty { "LAYOUT MOTION" },
                        description = strings["haptics.layout_desc"].ifEmpty { "Dodge-left and return-to-normal vibrations for back-button avoidance." },
                        onClick = { performHaptic(); section = "layout" },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        enabled = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 5, totalItems = 5) {
                    SettingsNavigationCard(
                        title = strings["haptics.reset_title"].ifEmpty { "RESET HAPTICS" },
                        description = strings["haptics.reset_desc"].ifEmpty { "Go back to GAMA's default premium vibration profile." },
                        onClick = { performHaptic(); section = "reset" },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        enabled = hapticsEnabled
                    )
                }
            }

            "core" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 6) {
                    HapticStrengthCard(
                        title = strings["haptics.regular_clicks"].ifEmpty { "REGULAR CLICKS" },
                        description = strings["haptics.regular_clicks_desc"].ifEmpty { "The light contact tick for normal settings cards and quick buttons. It should be clean, not buzzy." },
                        value = regularStrength,
                        onValueChange = onRegularStrengthChange,
                        enabled = regularEnabled,
                        onEnabledChange = onRegularEnabledChange,
                        preview = { GamaHaptics.lightClick(context, view) },
                        previewAtValue = { GamaHaptics.lightClick(context, view, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        isSmallScreen = isSmallScreen,
                        available = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 6) {
                    HapticStrengthCard(
                        title = strings["haptics.hold_release"].ifEmpty { "HOLD RELEASE BLOOM" },
                        description = strings["haptics.hold_release_desc"].ifEmpty { "The stronger second pulse after a deliberate press-and-hold. This should clearly contrast with a quick tap." },
                        value = holdStrength,
                        onValueChange = onHoldStrengthChange,
                        enabled = holdEnabled,
                        onEnabledChange = onHoldEnabledChange,
                        preview = { GamaHaptics.releaseAfterPress(context, view, SystemClock.uptimeMillis() - 320L, true) },
                        previewAtValue = { GamaHaptics.releaseAfterPress(context, view, SystemClock.uptimeMillis() - 320L, true, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        isSmallScreen = isSmallScreen,
                        available = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = false, staggerIndex = 4, totalItems = 5) {
                    HapticPreviewButton(
                        text = strings["haptics.press_hold_test"].ifEmpty { "TEST HAPTICS HERE" },
                        onClick = {},
                        onPress = { GamaHaptics.pressStart(context, view) },
                        onRelease = { startedAt, released ->
                            GamaHaptics.releaseAfterPress(context, view, startedAt, released)
                        },
                        colors = colors,
                        oledMode = oledMode,
                        enabled = hapticsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }

            "actions" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
                    HapticStrengthCard(
                        title = strings["haptics.renderer"].ifEmpty { "VULKAN / OPENGL" },
                        description = strings["haptics.renderer_desc"].ifEmpty { "A firmer renderer intent click. First contact is immediate; the commit pulse is more decisive." },
                        value = rendererStrength,
                        onValueChange = onRendererStrengthChange,
                        enabled = rendererEnabled,
                        onEnabledChange = onRendererEnabledChange,
                        preview = { GamaHaptics.rendererPressStart(context, view); GamaHaptics.rendererSelection(context, view) },
                        previewAtValue = { GamaHaptics.rendererSelection(context, view, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        isSmallScreen = isSmallScreen
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
                    HapticStrengthCard(
                        title = strings["haptics.language_change"].ifEmpty { "LANGUAGE CHANGE" },
                        description = strings["haptics.language_change_desc"].ifEmpty { "A small signature pattern when the app language actually changes." },
                        value = languageStrength,
                        onValueChange = onLanguageStrengthChange,
                        enabled = languageEnabled,
                        onEnabledChange = onLanguageEnabledChange,
                        preview = { GamaHaptics.languageChanged(context, view) },
                        previewAtValue = { GamaHaptics.languageChanged(context, view, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        isSmallScreen = isSmallScreen
                    )
                }

                AnimatedElement(visible = visible, cardShadow = false, staggerIndex = 4, totalItems = 5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HapticPreviewButton(
                            text = strings["haptics.vulkan_clone"].ifEmpty { "VULKAN CLONE" },
                            onClick = { GamaHaptics.rendererPressStart(context, view); GamaHaptics.rendererSelection(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPreviewButton(
                            text = strings["haptics.language_clone"].ifEmpty { "LANGUAGE CLONE" },
                            onClick = { GamaHaptics.languageChanged(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            "layout" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
                    SettingsNavigationCard(
                        title = strings["haptics.dodge_left"].ifEmpty { "DODGE LEFT" },
                        description = strings["haptics.dodge_left_desc"].ifEmpty { "Gentle tick when cards dodge away from the floating back button." },
                        onClick = { performHaptic(); section = "dodge" },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        enabled = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
                    SettingsNavigationCard(
                        title = strings["haptics.return_settle"].ifEmpty { "RETURN SETTLE" },
                        description = strings["haptics.return_settle_desc"].ifEmpty { "Stronger pulse when dodged cards return to normal width." },
                        onClick = { performHaptic(); section = "return" },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        enabled = hapticsEnabled
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HapticPreviewButton(
                            text = strings["haptics.dodge_preview"].ifEmpty { "DODGE" },
                            onClick = { GamaHaptics.avoidanceDodge(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            enabled = hapticsEnabled,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPreviewButton(
                            text = strings["haptics.return_preview"].ifEmpty { "RETURN" },
                            onClick = { GamaHaptics.avoidanceReturn(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            enabled = hapticsEnabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            "dodge" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 3) {
                    HapticStrengthCard(
                        title = strings["haptics.dodge_left"].ifEmpty { "DODGE LEFT" },
                        description = strings["haptics.dodge_left_desc"].ifEmpty { "Gentle tick when a card rescales or moves left to avoid the floating back button." },
                        value = bounceStrength,
                        onValueChange = onBounceStrengthChange,
                        enabled = bounceEnabled,
                        onEnabledChange = onBounceEnabledChange,
                        preview = { GamaHaptics.avoidanceDodge(context, view) },
                        previewAtValue = { GamaHaptics.avoidanceDodge(context, view, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        available = hapticsEnabled,
                        isSmallScreen = isSmallScreen
                    )
                }
            }

            "return" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 3) {
                    HapticStrengthCard(
                        title = strings["haptics.return_settle"].ifEmpty { "RETURN SETTLE" },
                        description = strings["haptics.return_settle_desc"].ifEmpty { "Stronger settling pulse when the card returns to normal width. This should feel like the UI snapping home." },
                        value = bounceReturnStrength,
                        onValueChange = onBounceReturnStrengthChange,
                        enabled = bounceReturnEnabled,
                        onEnabledChange = onBounceReturnEnabledChange,
                        preview = { GamaHaptics.avoidanceReturn(context, view) },
                        previewAtValue = { GamaHaptics.avoidanceReturn(context, view, it) },
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode,
                        available = hapticsEnabled,
                        isSmallScreen = isSmallScreen
                    )
                }
            }

            "preview" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HapticPreviewButton(
                            text = strings["haptics.quick_tap_preview"].ifEmpty { "QUICK TAP" },
                            onClick = { GamaHaptics.lightClick(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPreviewButton(
                            text = strings["haptics.vulkan_clone"].ifEmpty { "VULKAN" },
                            onClick = { GamaHaptics.rendererPressStart(context, view); GamaHaptics.rendererSelection(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HapticPreviewButton(
                            text = strings["haptics.language_clone"].ifEmpty { "LANGUAGE" },
                            onClick = { GamaHaptics.languageChanged(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                        HapticPreviewButton(
                            text = strings["haptics.return_preview"].ifEmpty { "RETURN" },
                            onClick = { GamaHaptics.avoidanceReturn(context, view) },
                            colors = colors,
                            oledMode = oledMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
                    HapticPreviewButton(
                        text = strings["haptics.press_hold_test"].ifEmpty { "PRESS & HOLD TEST" },
                        onClick = {},
                        onPress = { GamaHaptics.pressStart(context, view) },
                        onRelease = { startedAt, released ->
                            GamaHaptics.releaseAfterPress(context, view, startedAt, released)
                        },
                        colors = colors,
                        oledMode = oledMode,
                        enabled = hapticsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            "reset" -> {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4) {
                    SettingsNavigationCard(
                        title = strings["haptics.restore_default"].ifEmpty { "RESTORE DEFAULT PROFILE" },
                        description = strings["haptics.restore_default_desc"].ifEmpty { "Resets all haptic toggles and strengths to the default GAMA feel." },
                        onClick = {
                            onResetHaptics()
                            GamaHaptics.success(context, view)
                            section = "overview"
                        },
                        isSmallScreen = isSmallScreen,
                        colors = colors,
                        cardBackground = cardBackground,
                        oledMode = oledMode
                    )
                }

                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4) {
                    PanelCaption(
                        text = strings["haptics.defaults_note"].ifEmpty { "Defaults are tuned for a flagship-style linear vibration motor: light quick taps, clear hold bloom, firmer renderer commits, and separate dodge/return motion." },
                        colors = colors
                    )
                }
            }
        }
        }
    }
}

