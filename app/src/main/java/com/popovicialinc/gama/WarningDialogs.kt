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
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.positionChange
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



@Composable
fun WarningDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    killLauncher: Boolean = false,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color
) {
    val ts = LocalTypeScale.current
    val dialogShape = RoundedCornerShape(28.dp)

    BouncyDialog(visible = visible, onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape && !isTablet) 0.6f else 0.9f)
                .widthIn(max = 480.dp)
                .directionalShadow(cornerRadius = 28.dp)
                .border(1.dp, colors.primaryAccent.copy(alpha = 0.55f), dialogShape)
                .pointerInput(Unit) { detectTapGestures { } },
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = dialogShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isSmallScreen) 22.dp else 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = LocalStrings.current["dialogs.warning_title"].ifEmpty { "Just a sec!" },
                    fontSize = ts.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (killLauncher)
                        LocalStrings.current["dialogs.warning_body_with_launcher"].ifEmpty { "GAMA needs to briefly restart System UI, the launcher, and a few helper processes to apply this change." }
                    else
                        LocalStrings.current["dialogs.warning_body_no_launcher"].ifEmpty { "GAMA needs to briefly restart System UI and a few helper processes to apply this change." },
                    fontSize = ts.bodyLarge,
                    lineHeight = (ts.bodyLarge.value * 1.35f).sp,
                    color = colors.textSecondary,
                    fontFamily = quicksandFontFamily,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DialogButton(
                        text = LocalStrings.current["dialogs.btn_cancel"].ifEmpty { "Cancel" },
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        cardBackground = cardBackground,
                        borderAlphaOverride = 0.55f
                    )
                    DialogButton(
                        text = LocalStrings.current["dialogs.btn_continue"].ifEmpty { "Continue" },
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        cardBackground = cardBackground,
                        accent = true,
                        borderAlphaOverride = 0.55f
                    )
                }
            }
        }
    }
}

@Composable
fun AggressiveWarningDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isSmallScreen: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean = false, // Added
    dontShowAgain: Boolean = false,
    onDontShowAgainChange: (Boolean) -> Unit = {}
) {
    val ts = LocalTypeScale.current
    // Dialog Content - relies on main blur system (showAggressiveWarning is in anyPanelOpen)
    BouncyDialog(
        visible = visible,
        onDismiss = onDismiss
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 500.dp)
                    .directionalShadow(cornerRadius = 28.dp)
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.55f), RoundedCornerShape(40.dp))
                    .pointerInput(Unit) { /* Consume taps */ },
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isSmallScreen) 24.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = LocalStrings.current["dialogs.aggressive_title"].ifEmpty { "Aggressive Mode Warning ⚠️" },
                        fontSize = ts.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quicksandFontFamily,
                        color = colors.primaryAccent
                    )

                    Text(
                        text = LocalStrings.current["dialogs.aggressive_intro"].ifEmpty { "Using Aggressive mode is powerful, sure, but comes with some side effects that you should know about:" },
                        fontSize = ts.bodyMedium,
                        color = colors.textPrimary,
                        fontFamily = quicksandFontFamily,
                        fontWeight = FontWeight.Bold // Force Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🛑", fontSize = ts.buttonLarge)
                            Column {
                                Text(
                                    "Resets Defaults",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontFamily = quicksandFontFamily
                                )
                                Text(
                                    "Your default browser and keyboard will be reset",
                                    fontSize = ts.labelMedium,
                                    color = colors.textSecondary,
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📵", fontSize = ts.buttonLarge)
                            Column {
                                Text(
                                    "Connectivity Issues",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontFamily = quicksandFontFamily
                                )
                                Text(
                                    "Loss of WiFi-Calling/VoLTE capability. Fix: Settings → Connections → SIM manager, toggle SIM off and back on",
                                    fontSize = ts.labelMedium,
                                    color = colors.textSecondary,
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Added third warning row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("☠️", fontSize = ts.buttonLarge)
                            Column {
                                Text(
                                    "... and other stuff we haven't yet documented",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAccent, // Accented color
                                    fontFamily = quicksandFontFamily
                                )
                                Text(
                                    "ARE YOU CERTAIN WHATEVER YOU'RE DOING IS WORTH IT?",
                                    fontSize = ts.labelMedium,
                                    color = colors.textSecondary,
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "This mode is NOT recommended. If you're just pushing buttons to see what they do, don't mess with this.",
                                    fontSize = ts.labelMedium,
                                    color = colors.textSecondary,
                                    fontFamily = quicksandFontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    }

                    // "Don't show again" checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDontShowAgainChange(!dontShowAgain) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { onDontShowAgainChange(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primaryAccent,
                                uncheckedColor = colors.textSecondary
                            )
                        )
                        Text(
                            text = LocalStrings.current["dialogs.aggressive_dont_show_again"].ifEmpty { "Don't show this warning again" },
                            fontSize = ts.bodyMedium,
                            color = colors.textSecondary,
                            fontFamily = quicksandFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DialogButton(
                            text = LocalStrings.current["dialogs.btn_cancel"].ifEmpty { "Cancel" },
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = colors,
                            cardBackground = cardBackground,
                            accent = false,
                            oledMode = oledMode
                        )
                        DialogButton(
                            text = LocalStrings.current["dialogs.btn_ok"].ifEmpty { "OK" },
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = colors,
                            cardBackground = cardBackground,
                            accent = true,
                            oledMode = oledMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GPUWatchConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isSmallScreen: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    oledMode: Boolean = false
) {
    val ts = LocalTypeScale.current
    val dialogShape = RoundedCornerShape(28.dp)

    BouncyDialog(visible = visible, onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 450.dp)
                .directionalShadow(cornerRadius = 28.dp)
                .border(1.dp, colors.primaryAccent.copy(alpha = 0.55f), dialogShape)
                .pointerInput(Unit) { detectTapGestures { } },
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = dialogShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isSmallScreen) 22.dp else 26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LocalStrings.current["dialogs.gpuwatch_title"].ifEmpty { "Open GPUWatch" },
                    fontSize = ts.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = LocalStrings.current["dialogs.gpuwatch_body"].ifEmpty { "GAMA cannot open GPUWatch directly. It will open Developer Options, where you can enable GPUWatch yourself." },
                    fontSize = ts.bodyLarge,
                    color = colors.textSecondary,
                    fontFamily = quicksandFontFamily,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    lineHeight = (ts.bodyLarge.value * 1.35f).sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DialogButton(
                        text = LocalStrings.current["dialogs.btn_cancel"].ifEmpty { "Cancel" },
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        cardBackground = cardBackground,
                        accent = false,
                        oledMode = oledMode,
                        borderAlphaOverride = 0.55f
                    )
                    DialogButton(
                        text = LocalStrings.current["dialogs.btn_open"].ifEmpty { "Open" },
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        cardBackground = cardBackground,
                        accent = true,
                        oledMode = oledMode,
                        borderAlphaOverride = 0.55f
                    )
                }
            }
        }
    }
}

