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
fun RealtimeBlurBox(
    modifier: Modifier = Modifier,
    tintColor: Color = Color.Black.copy(alpha = 0.30f),
    blurRadius: Float = 12f,          // blur radius applied to the 1/4-res capture
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    borderColor: Color = Color.Transparent,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // Pre-API-26 fallback: plain tinted box, no blur
        Box(
            modifier = modifier
                .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
                .clip(shape)
                .background(tintColor),
            content = content
        )
        return
    }

    val view  = LocalView.current

    // Mutable state holding the latest blurred bitmap to paint as background
    var blurredBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Track the position and size of this box in window coordinates
    var windowX  by remember { mutableStateOf(0) }
    var windowY  by remember { mutableStateOf(0) }
    var boxW     by remember { mutableStateOf(0) }
    var boxH     by remember { mutableStateOf(0) }

    // ── Cached blur Paint — allocated ONCE, never per capture ────────────────
    // Previously: new android.graphics.Paint() + BlurMaskFilter() were allocated
    // inside the PixelCopy callback on every ~33ms capture, at ~30fps = ~30 Paint
    // + ~30 BlurMaskFilter allocations per second per RealtimeBlurBox instance.
    // Each triggers a GC pause on old devices. Cached here; BlurMaskFilter only
    // rebuilt when blurRadius changes (never at runtime).
    val cachedBlurPaint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }
    var cachedBlurRadiusApplied = remember { -1f }

    // Background coroutine: capture → scale-down → blur → publish at ~30 fps
    LaunchedEffect(Unit) {
        while (isActive) {
            val w = boxW; val h = boxH
            if (w > 4 && h > 4 && view.isAttachedToWindow) {
                // 1/4 resolution capture target
                val capW = (w / 4).coerceAtLeast(2)
                val capH = (h / 4).coerceAtLeast(2)
                val dest = Bitmap.createBitmap(capW, capH, Bitmap.Config.ARGB_8888)
                val srcRect = AndroidRect(windowX, windowY, windowX + w, windowY + h)

                try {
                    val window = (view.context as? android.app.Activity)?.window
                    if (window != null) {
                        // PixelCopy is async; suspend until callback fires
                        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                            PixelCopy.request(
                                window, srcRect, dest,
                                { result ->
                                    if (result == PixelCopy.SUCCESS) {
                                        // Rebuild BlurMaskFilter only when radius changes
                                        val clampedRadius = blurRadius.coerceAtLeast(1f)
                                        if (clampedRadius != cachedBlurRadiusApplied) {
                                            cachedBlurRadiusApplied = clampedRadius
                                            cachedBlurPaint.maskFilter = BlurMaskFilter(
                                                clampedRadius, BlurMaskFilter.Blur.NORMAL
                                            )
                                        }
                                        val blurred = Bitmap.createBitmap(capW, capH, Bitmap.Config.ARGB_8888)
                                        val canvas  = android.graphics.Canvas(blurred)
                                        canvas.drawBitmap(dest, 0f, 0f, cachedBlurPaint)
                                        dest.recycle()
                                        blurredBitmap = blurred.asImageBitmap()
                                    } else {
                                        dest.recycle()
                                    }
                                    if (cont.isActive) cont.resume(Unit) {}
                                },
                                android.os.Handler(android.os.Looper.getMainLooper())
                            )
                        }
                    } else {
                        dest.recycle()
                    }
                } catch (_: Exception) {
                    dest.recycle()
                }
            }
            delay(33L) // ~30 fps
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                windowX = pos.x.toInt()
                windowY = pos.y.toInt()
                boxW    = coords.size.width
                boxH    = coords.size.height
            }
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
            .clip(shape)
            .drawBehind {
                // Draw the blurred capture (scaled back up to full size) as background
                blurredBitmap?.let { bmp ->
                    drawImage(
                        image  = bmp,
                        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    )
                }
                // Tint overlay on top of the blur
                drawRect(color = tintColor)
            }
    ) {
        content()
    }
}

suspend fun getAllInstalledPackages(context: Context): List<Pair<String, String>> {
    val pm = context.packageManager

    // Try Shizuku first — gets every package regardless of visibility rules
    val shellPackages = ShizukuHelper.getAllPackageNames()

    if (shellPackages.isNotEmpty()) {
        return shellPackages.map { pkg ->
            val label = try {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
            } catch (_: Exception) {
                // PM can't see this package due to visibility filtering.
                // Use the package name — it's still shown and can be excluded.
                pkg
            }
            pkg to label
        }.sortedBy { it.second.lowercase() }
    }

    // Shizuku unavailable — fall back to PackageManager (partial list on API 30+)
    return pm.getInstalledApplications(PackageManager.GET_META_DATA).map {
        it.packageName to it.loadLabel(pm).toString()
    }.sortedBy { it.second.lowercase() }
}

