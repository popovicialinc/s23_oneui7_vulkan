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
fun FunctionalityPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRendererClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onCrashLogClick: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean,
    performHaptic: () -> Unit
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors
    ) { _ ->
        CleanTitle(
            text = LocalStrings.current["renderer.title"].ifEmpty { "RENDERER" },
            fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors = colors
        )

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 5) {
            SettingsNavigationCard(
                title = LocalStrings.current["renderer.title"].ifEmpty { "SWITCHING ENGINE" },
                description = LocalStrings.current["renderer.aggressive_mode_desc"].ifEmpty { "Aggressive mode, launcher restart, doze, and GPU Watch shortcut" },
                onClick = { performHaptic(); onRendererClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
            SettingsNavigationCard(
                title = LocalStrings.current["renderer.behavior_title"].ifEmpty { "BEHAVIOUR" },
                description = LocalStrings.current["renderer.verbose_mode_desc"].ifEmpty { "Verbose output, tap-outside dismissal, and panel interaction tweaks" },
                onClick = { performHaptic(); onBehaviorClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
            SettingsNavigationCard(
                title = LocalStrings.current["notifications.title"].ifEmpty { "NOTIFICATIONS" },
                description = LocalStrings.current["system.notifications_desc"].ifEmpty { "Reminder alerts if you've been on OpenGL longer than intended" },
                onClick = { performHaptic(); onNotificationsClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
            SettingsNavigationCard(
                title = LocalStrings.current["system.backup"].ifEmpty { "BACKUP & RESTORE" },
                description = LocalStrings.current["system.backup_desc"].ifEmpty { "Export all settings to a file or restore from a previous backup" },
                onClick = { performHaptic(); onBackupClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 5, totalItems = 5) {
            SettingsNavigationCard(
                title = LocalStrings.current["crash_log.title"].ifEmpty { "LOGS" },
                description = LocalStrings.current["system.crash_log_desc"].ifEmpty { "View recent reports and copy them for troubleshooting" },
                onClick = { performHaptic(); onCrashLogClick() },
                isSmallScreen = isSmallScreen, colors = colors,
                cardBackground = cardBackground, oledMode = oledMode
            )
        }
    }
}

@Composable
fun RendererPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    isRootPanel: Boolean = false,
    aggressiveMode: Boolean,
    onAggressiveModeChange: (Boolean) -> Unit,
    killLauncher: Boolean,
    onKillLauncherChange: (Boolean) -> Unit,
    killKeyboard: Boolean,
    onKillKeyboardChange: (Boolean) -> Unit,
    dozeMode: Boolean,
    onDozeModeChange: (Boolean) -> Unit,
    showGpuWatchButton: Boolean,
    onShowGpuWatchButtonChange: (Boolean) -> Unit,
    onShowAggressiveWarning: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean,
    performHaptic: () -> Unit
) {
    val ts = LocalTypeScale.current
    PanelScaffold(
        visible = visible, onDismiss = onDismiss,
        isLandscape = isLandscape, isSmallScreen = isSmallScreen,
        oledMode = oledMode, colors = colors,
        rootExitCascade = true,
    ) { _ ->
        CleanTitle(
            text = LocalStrings.current["renderer.title"].ifEmpty { "RENDERER" },
            fontSize = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors = colors
        )

        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 5) {
            ToggleCard(
                title = LocalStrings.current["renderer.aggressive_mode"].ifEmpty { "AGGRESSIVE MODE" },
                description = LocalStrings.current["renderer.aggressive_mode_desc"].ifEmpty { "Applies the renderer to every installed package — broader coverage, but read the warning before enabling" },
                checked = aggressiveMode,
                onCheckedChange = { performHaptic(); onAggressiveModeChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                accentBorder = true
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 5) {
            ToggleCard(
                title = LocalStrings.current["renderer.kill_launcher"].ifEmpty { "RESTART LAUNCHER ON SWITCH" },
                description = LocalStrings.current["renderer.kill_launcher_desc"].ifEmpty { "Force-stops the launcher after switching so it picks up the new renderer immediately — leave off on Xiaomi / MIUI" },
                checked = killLauncher,
                onCheckedChange = { performHaptic(); onKillLauncherChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                accentBorder = true
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 3, totalItems = 5) {
            ToggleCard(
                title = LocalStrings.current["renderer.kill_keyboard"].ifEmpty { "RESTART KEYBOARD ON SWITCH" },
                description = LocalStrings.current["renderer.kill_keyboard_desc"].ifEmpty { "Force-stops the currently selected keyboard after applying the renderer, so it reloads with the new graphics API" },
                checked = killKeyboard,
                onCheckedChange = { performHaptic(); onKillKeyboardChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                accentBorder = true
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 4, totalItems = 5) {
            ToggleCard(
                title = LocalStrings.current["renderer.doze_mode"].ifEmpty { "DOZE" },
                description = LocalStrings.current["renderer.doze_mode_desc"].ifEmpty { "Puts the device into deep sleep immediately — squeezes extra battery life when you're not using it" },
                checked = dozeMode,
                onCheckedChange = { performHaptic(); onDozeModeChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                accentBorder = true
            )
        }
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 5, totalItems = 5) {
            ToggleCard(
                title = LocalStrings.current["renderer.show_gpuwatch_toggle"].ifEmpty { "GPUWATCH SHORTCUT" },
                description = LocalStrings.current["renderer.show_gpuwatch_desc"].ifEmpty { "Adds an Open GPUWatch button on the main screen. Samsung devices only." },
                checked = showGpuWatchButton,
                onCheckedChange = { performHaptic(); onShowGpuWatchButtonChange(it) },
                colors = colors, cardBackground = cardBackground,
                isSmallScreen = isSmallScreen, oledMode = oledMode,
                accentBorder = true
            )
        }
    }
}

