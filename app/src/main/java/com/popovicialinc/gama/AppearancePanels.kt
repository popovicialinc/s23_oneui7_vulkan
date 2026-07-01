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
fun VisualEffectsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    themePreference: Int,
    onThemeChange: (Int) -> Unit,
    uiScale: Int,
    onUiScaleChange: (Int) -> Unit,
    userName: String,
    onUserNameChange: (String) -> Unit,
    oledMode: Boolean,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    shadowsEnabled: Boolean,
    onShadowsEnabledChange: (Boolean) -> Unit,
    onEffectsClick: () -> Unit,
    onColorsClick: () -> Unit,
    onAnimationsClick: () -> Unit,
    onOledModeChange: (Boolean) -> Unit,
    performHaptic: () -> Unit
) {
    val ts = LocalTypeScale.current

    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { scrollState ->
        CleanTitle(
            text = LocalStrings.current["particles.appearance_title"].ifEmpty { "APPEARANCE" },
            fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors = colors, scrollOffset = scrollState.value
        )

        // EFFECTS — top card
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 6) {
            SettingsNavigationCard(
                title = LocalStrings.current["effects.title"].ifEmpty { "EFFECTS" },
                description = LocalStrings.current["effects.effects_desc"].ifEmpty { "Background gradient, frosted glass blur, and floating particles" },
                onClick = { performHaptic(); onEffectsClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }

        // ANIMATIONS — sits below EFFECTS
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 6) {
            SettingsNavigationCard(
                title = "ANIMATIONS",
                description = "Animation quality, speed, and motion preferences",
                onClick = { performHaptic(); onAnimationsClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }

        // COLORS
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 6) {
            SettingsNavigationCard(
                title = LocalStrings.current["colors.title"].ifEmpty { "COLORS" },
                description = LocalStrings.current["colors.colors_desc"].ifEmpty { "Accent color, gradient palette, and Material You theming" },
                onClick = { performHaptic(); onColorsClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }

        // Theme — always available. Dark mode itself now uses pure OLED black.
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp))
            ) {
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
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = LocalStrings.current["appearance.theme"].ifEmpty { "THEME" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        GlideOptionSelector(
                            options = listOf(tr("appearance.theme_auto", "Auto"), tr("appearance.theme_dark", "Dark"), tr("appearance.theme_light", "Light")),
                            selectedIndex = themePreference,
                            onOptionSelected = { performHaptic(); onThemeChange(it) },
                            colors = colors, modifier = Modifier.fillMaxWidth(),
                            enabled = true
                        )
                    }
                }
            }
        }

        // UI Scale
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 5, totalItems = 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp))
            ) {
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
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = LocalStrings.current["appearance.ui_scale"].ifEmpty { "UI SCALE" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        GlideOptionSelector(
                            options = listOf("75%", "100%", "125%"),
                            selectedIndex = uiScale,
                            onOptionSelected = { performHaptic(); onUiScaleChange(it) },
                            colors = colors, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Your Name
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 6, totalItems = 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colors.primaryAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(28.dp))
            ) {
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
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = LocalStrings.current["appearance.your_name"].ifEmpty { "YOUR NAME" }, fontSize = ts.labelLarge, fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp, fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.7f)
                        )
                        Text(
                            text = LocalStrings.current["appearance.your_name_desc"].ifEmpty { "Used in greetings and notifications — completely optional" },
                            fontSize = ts.bodySmall, color = colors.textSecondary,
                            fontFamily = quicksandFontFamily, fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { onUserNameChange(it) },
                            placeholder = {
                                Text(
                                    "e.g. Alex",
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = ts.bodyMedium,
                                    color = colors.textSecondary.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = quicksandFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = ts.bodyMedium,
                                color = colors.textPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = colors.primaryAccent,
                                unfocusedBorderColor = colors.border.copy(alpha = 0.4f),
                                cursorColor          = colors.primaryAccent
                            ),
                            shape  = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OLEDPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    oledMode: Boolean,
    onOledModeChange: (Boolean) -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(text = LocalStrings.current["oled_panel.title"].ifEmpty { "OLED MODE" }, fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge, colors = colors)
        PanelCaption(
            text = LocalStrings.current["oled_panel.subtitle"].ifEmpty { "On OLED and AMOLED screens, true black pixels draw zero power — enabling this can meaningfully extend battery life" },
            colors = colors
        )
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 1) {
            ToggleCard(
                title = LocalStrings.current["colors.oled_mode"].ifEmpty { "OLED MODE" },
                description = if (oledMode) "Active — all backgrounds are pure black" else "Off — using your regular theme background color",
                checked = oledMode,
                onCheckedChange = { performHaptic(); onOledModeChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode
            )
        }
    }
}

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

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4) {
            ToggleCard(
                title = LocalStrings.current["blur.blur_toggle"].ifEmpty { "BLUR" },
                description = LocalStrings.current["blur.blur_toggle_desc"].ifEmpty { "Frosted glass behind panels and dialogs — subtle depth that makes the UI feel premium" },
                checked = blurEnabled,
                onCheckedChange = { performHaptic(); onBlurChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4) {
            ToggleCard(
                title = LocalStrings.current["appearance.card_shadows"].ifEmpty { "CARD SHADOWS" },
                description = LocalStrings.current["appearance.card_shadows_desc"].ifEmpty { "Drop shadows under cards — disable to reduce GPU load or fix visual glitches during animations" },
                checked = shadowsEnabled && !oledMode,
                onCheckedChange = { performHaptic(); onShadowsEnabledChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                enabled = !oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 4) {
            SettingsNavigationCard(
                title = LocalStrings.current["particles.toggle"].ifEmpty { "PARTICLES" }, description = LocalStrings.current["effects.particles_desc"].ifEmpty { "Floating dots, twinkling stars, or Matrix rain — choose your style" },
                onClick = { performHaptic(); onParticlesClick() },
                isSmallScreen = isSmallScreen, colors = colors, cardBackground = cardBackground, oledMode = oledMode
            )
        }
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

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 3, enabled = dynamicColorAvailable) {
            ToggleCard(
                title = LocalStrings.current["colors.dynamic_color"].ifEmpty { "DYNAMIC COLOR" },
                description = if (dynamicColorAvailable) LocalStrings.current["colors.dynamic_color_desc"].ifEmpty { "Picks accent colors from your wallpaper automatically via Material You — also colors the Matrix rain when active" } else LocalStrings.current["colors.dynamic_color_unavailable"].ifEmpty { "Requires Android 12 or newer" },
                checked = useDynamicColor && dynamicColorAvailable,
                onCheckedChange = { performHaptic(); onDynamicColorChange(it) },
                colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen,
                oledMode = oledMode, enabled = dynamicColorAvailable
            )
        }
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 3,
                enabled = !useDynamicColor || !dynamicColorAvailable) {
            ToggleCard(
                title = LocalStrings.current["colors.advanced_picker"].ifEmpty { "HEX COLOR PICKER" },
                description = LocalStrings.current["colors.advanced_picker_desc"].ifEmpty { "Adds a hex input field to the color pickers — type any color directly, e.g. #4895EF" },
                checked = advancedColorPicker,
                onCheckedChange = { performHaptic(); onAdvancedColorPickerChange(it) },
                colors = colors, cardBackground = cardBackground, isSmallScreen = isSmallScreen,
                oledMode = oledMode, enabled = !useDynamicColor || !dynamicColorAvailable
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 3,
            enabled = !useDynamicColor || !dynamicColorAvailable) {
            CompactColorPickerCard(
                title = LocalStrings.current["colors.accent_color"].ifEmpty { "ACCENT COLOR" },
                description = LocalStrings.current["colors.accent_color_desc"].ifEmpty { "The highlight color used on buttons, borders, and interactive elements." },
                currentColor = customAccentColor,
                onColorChange = onAccentColorChange,
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, isLandscape = isLandscape,
                advancedPicker = advancedColorPicker,
                enabled = !useDynamicColor || !dynamicColorAvailable,
                oledMode = oledMode,
                filterExtremeForTheme = true,
                isDarkTheme = oledMode
            )
        }
    }
}
