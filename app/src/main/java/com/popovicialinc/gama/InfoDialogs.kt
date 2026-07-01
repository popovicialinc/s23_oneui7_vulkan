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
fun ShizukuHelpDialog(
    visible: Boolean,
    helpType: String,
    onDismiss: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color
) {
    val ts = LocalTypeScale.current
    val dialogBorderAlpha = 0.55f  // matches SettingsNavigationCard (APPEARANCE button)
    val dialogBorderWidth = 1.dp
    val dialogShape = RoundedCornerShape(40.dp)

    BouncyDialog(visible = visible, onDismiss = onDismiss) {
    Card(
        modifier = Modifier
            .fillMaxWidth(if (isLandscape && !isTablet) 0.6f else 0.9f)
            .widthIn(max = 500.dp)
            .directionalShadow(cornerRadius = 28.dp)
            .border(
                width = dialogBorderWidth,
                color = colors.primaryAccent.copy(alpha = dialogBorderAlpha),
                shape = dialogShape
            )
            .pointerInput(Unit) {
                detectTapGestures { /* Block taps on card from dismissing dialog */ }
            },
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = dialogShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 22.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 18.dp else 22.dp)
        ) {
            // Title — accent-coloured, matches ExternalLinkConfirmDialog
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (helpType == "not_running") "Shizuku Not Running" else "Permission Needed",
                    fontSize = ts.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily,
                    color = colors.primaryAccent
                )
            }

            Text(
                text = when (helpType) {
                    "not_running" -> "Shizuku needs to be running for GAMA to work.\n\n1. Open the Shizuku app\n2. Tap 'Start' to activate the service\n3. Return to GAMA\n\nIf Shizuku won't start, follow the wireless debugging instructions in the Shizuku app."
                    "permission" -> "GAMA needs permission to use Shizuku.\n\n1. Open Shizuku\n2. Tap 'Authorized application'\n3. Find GAMA and enable it\n4. Close GAMA from your recents\n5. Reopen GAMA\n"
                    else -> "Unknown error"
                },
                fontSize = ts.bodyLarge,
                lineHeight = (ts.bodyLarge.value * 1.4f).sp,
                color = colors.textPrimary.copy(alpha = 0.85f),
                fontFamily = quicksandFontFamily,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            // Button border uses the same accent alpha as the card outline for consistency
            DialogButton(
                text = LocalStrings.current["dialogs.btn_okay"].ifEmpty { "Okay" },
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = colors,
                cardBackground = cardBackground,
                accent = true,
                borderAlphaOverride = dialogBorderAlpha
            )
        }
    }
    } // BouncyDialog
}

@Composable
fun EasterEggDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    colors: ThemeColors,
    cardBackground: Color,
    isSmallScreen: Boolean
) {
    val ts = LocalTypeScale.current
    val animLevel = LocalAnimationLevel.current
    BouncyDialog(visible = visible, onDismiss = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isSmallScreen) 0.90f else 0.84f)
                    .widthIn(max = 460.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.primaryAccent.copy(alpha = 0.6f),
                                    colors.primaryAccent.copy(alpha = 0.15f),
                                    colors.primaryAccent.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(40.dp)
                        )
                        .clip(RoundedCornerShape(40.dp))
                ) {
                    // ── Ambient glow blob at the top — only runs while dialog is visible ──
                    val glowPulse = rememberInfiniteTransition(label = "egg_glow")
                    val glowAlpha by glowPulse.animateFloat(
                        initialValue = if (visible) 0.28f else 0f,
                        targetValue  = if (visible) 0.48f else 0f,
                        animationSpec = infiniteRepeatable(
                            tween(2200, easing = MotionTokens.Easing.silk),
                            RepeatMode.Reverse
                        ), label = "egg_glow_a"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .blur(60.dp, BlurredEdgeTreatment.Unbounded)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colors.primaryAccent.copy(alpha = glowAlpha),
                                            colors.primaryAccent.copy(alpha = glowAlpha * 0.3f),
                                            Color.Transparent
                                        ),
                                        radius = 400f
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colors.primaryAccent.copy(alpha = glowAlpha * 0.45f),
                                            Color.Transparent
                                        ),
                                        radius = 400f
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (isSmallScreen) 40.dp else 52.dp,
                                bottom = if (isSmallScreen) 32.dp else 40.dp,
                                start = if (isSmallScreen) 28.dp else 36.dp,
                                end = if (isSmallScreen) 28.dp else 36.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // ── GAMA wordmark with glow ───────────────────────────
                        val gamaSize = if (isSmallScreen) (ts.displayLarge.value * 1.4f).sp
                        else (ts.displayLarge.value * 1.7f).sp
                        val context = LocalContext.current
                        // Cache Paint+BlurMaskFilter — both are constant; no need to
                        // reconstruct them on every draw frame inside drawWithContent.
                        val easterGlowPaint = remember(colors.primaryAccent, gamaSize) {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.TRANSPARENT
                                maskFilter = android.graphics.BlurMaskFilter(
                                    100f, android.graphics.BlurMaskFilter.Blur.NORMAL
                                )
                                setShadowLayer(
                                    100f, 0f, 0f,
                                    colors.primaryAccent.copy(alpha = 0.7f).toArgb()
                                )
                            }
                        }
                        Text(
                            text = "GAMA",
                            fontSize = gamaSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = quicksandFontFamily,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.drawWithContent {
                                drawIntoCanvas { canvas ->
                                    easterGlowPaint.textSize = gamaSize.toPx()
                                    canvas.nativeCanvas.drawText(
                                        "GAMA",
                                        size.width / 2f,
                                        size.height / 2f + gamaSize.toPx() * 0.28f,
                                        easterGlowPaint
                                    )
                                }
                                drawContent()
                            }
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 6.dp else 8.dp))

                        // ── Thin accent divider ───────────────────────────────
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            colors.primaryAccent.copy(alpha = 0.7f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 20.dp else 28.dp))

                        // ── Main copy ─────────────────────────────────────────
                        Text(
                            text = "Graphics API Manager\nfor Android",
                            fontSize = ts.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = quicksandFontFamily,
                            color = colors.textPrimary.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            lineHeight = (ts.headlineSmall.value * 1.45f).sp
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 16.dp else 22.dp))

                        Text(
                            text = LocalStrings.current["dialogs.easter_egg_subtitle"].replace("\\n","\n").ifEmpty { "Built with obsessive attention to detail,\nlate nights, and too much hot cocoa." },
                            fontSize = ts.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = quicksandFontFamily,
                            color = colors.textSecondary.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            lineHeight = (ts.bodyMedium.value * 1.6f).sp
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 8.dp else 10.dp))

                        Text(
                            text = LocalStrings.current["dialogs.easter_egg_thanks"].ifEmpty { "Thanks for using it." },
                            fontSize = ts.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = quicksandFontFamily,
                            color = colors.primaryAccent.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 6.dp else 8.dp))

                        Text(
                            text = "@palincat",
                            fontSize = ts.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = quicksandFontFamily,
                            color = colors.textSecondary.copy(alpha = 0.38f),
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(Modifier.height(if (isSmallScreen) 28.dp else 36.dp))

                        // ── Dismiss button ────────────────────────────────────
                        DialogButton(
                            text = LocalStrings.current["dialogs.btn_nice"].ifEmpty { "❤️  Nice" },
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = colors,
                            cardBackground = cardBackground,
                            accent = true
                        )
                    }
                }
            }
        }
    }
}

