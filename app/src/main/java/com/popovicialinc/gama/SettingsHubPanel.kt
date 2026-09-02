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
fun SettingsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSearchClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onRendererClick: () -> Unit,
    onSystemClick: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts = LocalTypeScale.current
    val strings = LocalStrings.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors,
        rootExitCascade = true,
        leadingFloatingButton = { floatingModifier, holdState, isLeftSide ->
            PanelSearchButton(
                onClick = { performHaptic(); onSearchClick() },
                colors = colors,
                oledMode = oledMode,
                isSmallScreen = isSmallScreen,
                enabled = visible,
                floatingHoldState = holdState,
                isLeftSide = isLeftSide,
                modifier = floatingModifier
            )
        }
    ) { scrollState ->
        AnimatedElement(visible = visible, staggerIndex = 0, totalItems = 6) {
            CleanTitle(
                text = strings["settings.title"].ifEmpty { "SETTINGS" },
                fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
                colors = colors, scrollState = scrollState
            )
        }

        AnimatedElement(visible = visible, staggerIndex = 1, totalItems = 6) {
            PanelCaption(
                text = strings["settings.caption"]
                    .ifEmpty { strings["text_catalog.core_app_sections_use_search_to_jump_directly_to_toggles_sli"] }
                    .ifEmpty { "Core app sections. Use search to jump directly to toggles, sliders, and selectors." },
                colors = colors
            )
        }

        ResponsiveSettingsCardGrid(
            isLandscape = isLandscape,
            cards = listOf(
                {
                    AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 6) {
                        SettingsNavigationCard(
                            title = strings["settings.appearance"].ifEmpty { "VISUALS" },
                            description = strings["settings.appearance_desc"].ifEmpty { "Colors, theme, effects, animations, and interface scale" },
                            onClick = { performHaptic(); onAppearanceClick() },
                            isSmallScreen = isSmallScreen, colors = colors,
                            cardBackground = cardBackground, oledMode = oledMode
                        )
                    }
                },
                {
                    AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 6) {
                        SettingsNavigationCard(
                            title = strings["settings.renderer"].ifEmpty { "RENDERER" },
                            description = strings["settings.renderer_desc"].ifEmpty { "Switching engine, aggressive mode, launcher and keyboard behavior" },
                            onClick = { performHaptic(); onRendererClick() },
                            isSmallScreen = isSmallScreen, colors = colors,
                            cardBackground = cardBackground, oledMode = oledMode
                        )
                    }
                },
                {
                    AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
                        SettingsNavigationCard(
                            title = strings["settings.system"].ifEmpty { "SYSTEM" },
                            description = strings["settings.system_desc"].ifEmpty { "Notifications, backup, language, integrations, and logs" },
                            onClick = { performHaptic(); onSystemClick() },
                            isSmallScreen = isSmallScreen, colors = colors,
                            cardBackground = cardBackground, oledMode = oledMode
                        )
                    }
                }
            )
        )
    }
}

