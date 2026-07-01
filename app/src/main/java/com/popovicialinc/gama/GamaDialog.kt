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
import android.os.SystemClock
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.Rect as AndroidRect
import android.view.HapticFeedbackConstants
import android.view.PixelCopy
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.platform.LocalViewConfiguration
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
import androidx.compose.ui.text.style.TextOverflow
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
fun BouncyDialog(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    // Call the full implementation with default parameters
    BouncyDialog(
        visible = visible,
        onDismiss = onDismiss,
        fullScreen = false,
        applyBlur = false,
        content = content
    )
}

@Composable
fun BouncyDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
    applyBlur: Boolean = false,
    exitStartDelayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val animLevel = LocalAnimationLevel.current
    val animSpeed = LocalAnimationSpeed.current
    val dismissOnClickOutside = LocalDismissOnClickOutside.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    var renderContent by remember { mutableStateOf(visible) }
    var isExiting     by remember { mutableStateOf(false) }
    val animScale     = remember { Animatable(if (visible) 1f else 0.88f) }
    val animAlpha     = remember { Animatable(if (visible) 1f else 0f) }
    val scope         = rememberCoroutineScope()

    LaunchedEffect(visible) {
        if (visible) {
            isExiting = false
            renderContent = true
            if (animLevel == 2) {
                animScale.snapTo(1f)
                animAlpha.snapTo(1f)
                return@LaunchedEffect
            }
            animScale.snapTo(0.88f)
            animAlpha.snapTo(0f)
            val enterSpec: FiniteAnimationSpec<Float> = when (animLevel) {
                0 -> spring(dampingRatio = 0.42f, stiffness = MotionTokens.SpeedUtil.stiffness(190f, animSpeed))
                else -> tween(durationMillis = MotionTokens.SpeedUtil.durationMs(210, animSpeed), easing = MotionTokens.Easing.emphasizedDecelerate)
            }
            val alphaEnterDuration = when (animLevel) { 0 -> 280; else -> 190 }
            scope.launch {
                animScale.animateTo(targetValue = 1f, animationSpec = enterSpec)
            }
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = alphaEnterDuration, easing = MotionTokens.Easing.enter)
            )
        } else {
            isExiting = true
            if (animLevel == 2) {
                renderContent = false
                isExiting = false
                animScale.snapTo(0.88f)
                animAlpha.snapTo(0f)
                return@LaunchedEffect
            }
            if (exitStartDelayMillis > 0) {
                delay(exitStartDelayMillis.toLong())
            }
            val exitDuration = when (animLevel) { 0 -> 170; else -> 115 }
            scope.launch {
                animScale.animateTo(
                    targetValue = if (animLevel == 0) 0.78f else 0.96f,
                    animationSpec = tween(durationMillis = exitDuration, easing = MotionTokens.Easing.exit)
                )
            }
            animAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = (exitDuration - 20).coerceAtLeast(70), easing = MotionTokens.Easing.exit)
            )
            renderContent = false
            isExiting = false
            animScale.snapTo(0.88f)
            animAlpha.snapTo(0f)
        }
    }

    if (!renderContent) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (visible && !isExiting) Modifier.pointerInput(dismissOnClickOutside) {
                    if (dismissOnClickOutside) detectTapGestures { currentOnDismiss() }
                    else detectTapGestures { }
                } else Modifier
            )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
                alpha  = animAlpha.value
            },
        contentAlignment = if (fullScreen) Alignment.TopStart else Alignment.Center
    ) {
        content()
    }
}

