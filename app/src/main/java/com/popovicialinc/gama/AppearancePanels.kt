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
fun EffectsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    blurEnabled: Boolean,
    onBlurChange: (Boolean) -> Unit,
    shadowsEnabled: Boolean,
    onShadowsEnabledChange: (Boolean) -> Unit,
    onParticlesClick: () -> Unit,
    userName: String,
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
        CleanTitle(text = LocalStrings.current["effects.title"].ifEmpty { "EFFECTS" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)

        ResponsiveSettingsCardGrid(isLandscape, listOf(
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4) {
                    ToggleCard(title = LocalStrings.current["blur.blur_toggle"].ifEmpty { "BLUR" }, description = LocalStrings.current["blur.blur_toggle_desc"].ifEmpty { "Frosted glass behind panels and dialogs — subtle depth that makes the UI feel premium" }, checked = blurEnabled, onCheckedChange = { performHaptic(); onBlurChange(it) }, colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen, oledMode = oledMode)
                }
            },
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4) {
                    ToggleCard(title = LocalStrings.current["appearance.card_shadows"].ifEmpty { "CARD SHADOWS" }, description = LocalStrings.current["appearance.card_shadows_desc"].ifEmpty { "Drop shadows under cards — disable to reduce GPU load or fix visual glitches during animations" }, checked = shadowsEnabled && !oledMode, onCheckedChange = { performHaptic(); onShadowsEnabledChange(it) }, colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen, oledMode = oledMode, enabled = !oledMode)
                }
            },
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4) {
                    SettingsNavigationCard(title = LocalStrings.current["particles.toggle"].ifEmpty { "PARTICLES" }, description = LocalStrings.current["particles.toggle_desc"].ifEmpty { "Gives the app a living feel" }, onClick = { performHaptic(); onParticlesClick() }, isSmallScreen = isSmallScreen, colors = colors, cardBackground = cardBackground, oledMode = oledMode)
                }
            }
        ))
    }
}

@Composable
fun ColorCustomizationPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    oledMode: Boolean,
    onOledModeChange: (Boolean) -> Unit,
    useDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    advancedColorPicker: Boolean,
    onAdvancedColorPickerChange: (Boolean) -> Unit,
    customAccentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    onGradientClick: () -> Unit = {},
    isDarkTheme: Boolean,
    performHaptic: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color
) {
    val ts = LocalTypeScale.current
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(text = LocalStrings.current["colors.title"].ifEmpty { "COLORS" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)

        ResponsiveSettingsCardGrid(isLandscape, listOf(
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 3, enabled = dynamicColorAvailable) {
                    ToggleCard(title = LocalStrings.current["colors.dynamic_color"].ifEmpty { "DYNAMIC COLOR" }, description = if (dynamicColorAvailable) LocalStrings.current["colors.dynamic_color_desc"].ifEmpty { "Picks accent colors from your wallpaper automatically via Material You — also colors the Matrix rain when active" } else LocalStrings.current["colors.dynamic_color_unavailable"].ifEmpty { "Requires Android 12 or newer" }, checked = useDynamicColor && dynamicColorAvailable, onCheckedChange = { performHaptic(); onDynamicColorChange(it) }, colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen, oledMode = oledMode, enabled = dynamicColorAvailable)
                }
            },
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 3, enabled = !useDynamicColor || !dynamicColorAvailable) {
                    ToggleCard(title = LocalStrings.current["colors.advanced_picker"].ifEmpty { "HEX COLOR PICKER" }, description = LocalStrings.current["colors.advanced_picker_desc"].ifEmpty { "Adds a hex input field to the color pickers — type any color directly, e.g. #4895EF" }, checked = advancedColorPicker, onCheckedChange = { performHaptic(); onAdvancedColorPickerChange(it) }, colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen, oledMode = oledMode, enabled = !useDynamicColor || !dynamicColorAvailable)
                }
            },
            {
                AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 3, enabled = !useDynamicColor || !dynamicColorAvailable) {
                    CompactColorPickerCard(title = LocalStrings.current["colors.accent_color"].ifEmpty { "ACCENT COLOR" }, description = LocalStrings.current["colors.accent_color_desc"].ifEmpty { "The highlight color used on buttons, borders, and interactive elements." }, currentColor = customAccentColor, onColorChange = onAccentColorChange, colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen, isLandscape = isLandscape, advancedPicker = advancedColorPicker, enabled = !useDynamicColor || !dynamicColorAvailable, oledMode = oledMode, filterExtremeForTheme = true, isDarkTheme = oledMode)
                }
            }
        ))
    }
}
