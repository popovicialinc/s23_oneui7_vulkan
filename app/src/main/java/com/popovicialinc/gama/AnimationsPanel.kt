package com.popovicialinc.gama

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*

@Composable
fun AnimationsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    animationLevel: Int,
    onAnimationLevelChange: (Int) -> Unit,
    animSpeed: Int,
    onAnimSpeedChange: (Int) -> Unit,
    staggerEnabled: Boolean,
    onStaggerEnabledChange: (Boolean) -> Unit,
    backButtonAvoidanceEnabled: Boolean,
    onBackButtonAvoidanceEnabledChange: (Boolean) -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean,
    animationsEnabled: Boolean,
    onAnimationsEnabledChange: (Boolean) -> Unit
) {
    val ts = LocalTypeScale.current

    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        // Apply animations enabled composition local to all panel content
        CompositionLocalProvider(LocalAnimationsEnabled provides animationsEnabled) {
            CleanTitle(
                text = "ANIMATIONS",
                fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
                colors = colors
            )

            // ANIMATIONS TOGGLE - Main toggle at the top
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 0, totalItems = 5) {
                ToggleCard(
                    title = LocalStrings.current["appearance.animations_toggle"].ifEmpty { "ANIMATIONS" },
                    description = LocalStrings.current["appearance.animations_toggle_desc"].ifEmpty { "Enable or disable all UI animations" },
                    checked = animationsEnabled,
                    onCheckedChange = {
                        performHaptic()
                        onAnimationsEnabledChange(it)
                    },
                    colors = colors, cardBackground = cardBackground,
                    isSmallScreen = isSmallScreen, oledMode = oledMode
                )
            }

            // QUALITY — Full / Reduced
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 5) {
                ToggleCard(
                    title = "QUALITY",
                    description = LocalStrings.current["appearance.animation_quality"].ifEmpty { "Adjust the complexity of UI animations — Full for smooth motion, Reduced for performance" },
                    checked = false, // Not a toggle, just a selector card
                    onCheckedChange = {},
                    colors = colors, cardBackground = cardBackground,
                    isSmallScreen = isSmallScreen, oledMode = oledMode,
                    enabled = animationsEnabled,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "QUALITY",
                                fontSize = ts.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = quicksandFontFamily,
                                color = colors.primaryAccent.copy(alpha = 0.7f)
                            )
                            GlideOptionSelector(
                                // animationLevel is tri-state (0=Full, 1=Reduced, 2=Off)
                                // — same three options as VisualEffectsPanel; offering
                                // only two here made level==2 render as "Reduced".
                                options = listOf(
                                    LocalStrings.current["appearance.anim_full"].ifEmpty { "Full" },
                                    LocalStrings.current["appearance.anim_reduced"].ifEmpty { "Reduced" },
                                    LocalStrings.current["appearance.anim_off"].ifEmpty { "Off" }
                                ),
                                selectedIndex = animationLevel,
                                onOptionSelected = { performHaptic(); onAnimationLevelChange(it) },
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            // SPEED — Relaxed / Normal / Snappy
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
                ToggleCard(
                    title = "SPEED",
                    description = LocalStrings.current["appearance.animation_speed"].ifEmpty { "Control how fast animations play — Relaxed for gentle motion, Snappy for quick transitions" },
                    checked = false, // Not a toggle, just a selector card
                    onCheckedChange = {},
                    colors = colors, cardBackground = cardBackground,
                    isSmallScreen = isSmallScreen, oledMode = oledMode,
                    enabled = animationsEnabled,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SPEED",
                                fontSize = ts.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = quicksandFontFamily,
                                color = colors.primaryAccent.copy(alpha = 0.7f)
                            )
                            GlideOptionSelector(
                                options = listOf("Relaxed", "Normal", "Snappy"),
                                selectedIndex = animSpeed,
                                onOptionSelected = { performHaptic(); onAnimSpeedChange(it) },
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }

            // STAGGER ANIMATIONS
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
                ToggleCard(
                    title = LocalStrings.current["appearance.stagger_animations"].ifEmpty { "STAGGER ANIMATIONS" },
                    description = LocalStrings.current["appearance.stagger_animations_desc"].ifEmpty { "Panel cards animate in one by one — turn off for instant panel opens" },
                    checked = staggerEnabled,
                    onCheckedChange = { performHaptic(); onStaggerEnabledChange(it) },
                    colors = colors, cardBackground = cardBackground,
                    isSmallScreen = isSmallScreen, oledMode = oledMode,
                    enabled = animationsEnabled
                )
            }

            // BACK BUTTON AVOIDANCE
            AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
                ToggleCard(
                    title = LocalStrings.current["appearance.back_button_avoidance"].ifEmpty { "BACK BUTTON AVOIDANCE" },
                    description = LocalStrings.current["appearance.back_button_avoidance_desc"].ifEmpty { "Cards duck left when the floating < button would overlap them. Turn off to let the button float above the UI." },
                    checked = backButtonAvoidanceEnabled,
                    onCheckedChange = { performHaptic(); onBackButtonAvoidanceEnabledChange(it) },
                    colors = colors, cardBackground = cardBackground,
                    isSmallScreen = isSmallScreen, oledMode = oledMode,
                    enabled = animationsEnabled
                )
            }
        } // end CompositionLocalProvider
    }
}
