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
import androidx.compose.ui.graphics.drawscope.withTransform
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

private const val TRAIL_LUT_SIZE = 64

// ─────────────────────────────────────────────────────────────────────────────
// Internal data model — per-column mutable state.
// Written by the physics background thread; read by the Canvas main thread.
// No synchronisation needed: each column is only ever written by the physics
// loop and read by the Canvas in the same tick window; the worst that can
// happen is a one-frame-old read, which is invisible at 30-120 Hz.
// ─────────────────────────────────────────────────────────────────────────────
class MatrixColumn(
    val x: Float,            // horizontal centre of this column, px
    @Volatile var headRow: Int,  // head position in discrete row units (row * fontSizePx = Y)
    val ticksPerStep: Int,   // how many physics ticks between each 1-row downward step
    val chars: CharArray,    // ring of characters shown in the trail
    val trailSlots: Int,     // how many character rows the trail spans
    @Volatile var charAge: Int = 0,
    val shuffleEvery: Int,   // physics ticks between random character swaps
    @Volatile var tickAccum: Int = 0,  // counts ticks until next row step
    // ── 3-D depth ────────────────────────────────────────────────────────────
    // 0.0 = far away (small, slow, dim)  |  1.0 = close (large, fast, bright)
    val depth: Float = 1.0f
)

private val MATRIX_CHARS: CharArray = (
        "\u30A1\u30A2\u30A3\u30A4\u30A5\u30A6\u30A7\u30A8\u30A9\u30AA" + // ア-コ
                "\u30AB\u30AC\u30AD\u30AE\u30AF\u30B0\u30B1\u30B2\u30B3\u30B4" + // カ-ゴ
                "\u30B5\u30B6\u30B7\u30B8\u30B9\u30BA\u30BB\u30BC\u30BD\u30BE" + // サ-ゾ
                "\u30BF\u30C0\u30C1\u30C2\u30C3\u30C4\u30C5\u30C6\u30C7\u30C8" + // タ-ド
                "\u30C9\u30CA\u30CB\u30CC\u30CD\u30CE\u30CF\u30D0\u30D1\u30D2" + // ト-ヒ
                "\u30D3\u30D4\u30D5\u30D6\u30D7\u30D8\u30D9\u30DA\u30DB\u30DC" + // ビ-ボ
                "0123456789" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "@#\$%^&*-+=<>?!~|:;"
        ).toCharArray()

private fun randomMatrixChar(): Char =
    MATRIX_CHARS[kotlin.random.Random.nextInt(MATRIX_CHARS.size)]

// ─────────────────────────────────────────────────────────────────────────────
// MatrixRainOverlay — public composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MatrixRainOverlay(
    enabled: Boolean,

    // ── Visual ────────────────────────────────────────────────────────────────
    /** Colour of the leading (head) character — default bright white */
    headColor: Color = Color(0xFFFFFFFF),
    /** Colour of the characters just below the head — the "hot" part of the trail */
    rainColor: Color = Color(0xFF00FF41),
    /** Colour blended into the deep tail — the "cool" / dim end */
    trailColor: Color = Color(0xFF003B00),
    /** Solid background painted under every column.
     *  Use Color.Black + backgroundAlpha=1 for the pure cinema look,
     *  or Color.Transparent + backgroundAlpha=0 (default) to overlay on top of
     *  the existing app background. */
    backgroundColor: Color = Color.Black,
    /** 0 = fully transparent (composites over app), 1 = fully opaque black canvas */
    backgroundAlpha: Float = 0.0f,

    // ── Motion ────────────────────────────────────────────────────────────────
    /** 0 = slow, 1 = medium (default), 2 = fast */
    speedLevel: Int = 1,

    // ── Appearance ────────────────────────────────────────────────────────────
    /** 0 = sparse columns, 1 = medium (default), 2 = dense */
    densityLevel: Int = 1,
    /** 0 = small glyphs, 1 = medium (default), 2 = large */
    fontSizeLevel: Int = 1,
    /** 0 = short trail, 1 = medium (default), 2 = full-screen-length trail */
    fadeLength: Int = 1,

    // ── Performance ───────────────────────────────────────────────────────────
    // No refresh-rate knob needed: the rain steps one row at a time, so the
    // tick rate is derived entirely from speedLevel. The render loop fires
    // only when a step actually occurs — no wasted vsync budget.
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val animSpeed = LocalAnimationSpeed.current

    // Screen size in physical pixels — recomputed only on rotation/resize
    val screenWidthPx = remember(configuration) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    // Accent changes must never rebuild / respawn Matrix columns.
    // Columns are remembered below by geometry/speed settings only; these animated
    // colours are read by the Canvas draw code, so the rain stays alive and simply
    // eases into the new ACCENT COLOR.
    val renderedHeadColor by animateColorAsState(
        targetValue = headColor,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "matrix_head_render_color"
    )
    val renderedRainColor by animateColorAsState(
        targetValue = rainColor,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "matrix_rain_render_color"
    )
    val renderedTrailColor by animateColorAsState(
        targetValue = trailColor,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "matrix_trail_render_color"
    )

    // ── Derive numeric parameters from the 0/1/2 levels ──────────────────────
    val fontSizePx: Float = remember(fontSizeLevel, density) {
        with(density) {
            when (fontSizeLevel) {
                0 -> 11.sp.toPx()
                2 -> 20.sp.toPx()
                else -> 15.sp.toPx()
            }
        }
    }

    // Column width keeps cells roughly square; density just packs them tighter
    val columnWidth: Float = remember(fontSizePx, densityLevel) {
        when (densityLevel) {
            0 -> fontSizePx * 2.0f   // sparse  — wide gaps
            2 -> fontSizePx * 1.1f   // dense   — nearly touching
            else -> fontSizePx * 1.5f   // medium
        }
    }

    // Number of visible character slots per column (trail + head)
    val trailLength: Int = remember(fadeLength, screenHeightPx, fontSizePx) {
        val fullScreen = (screenHeightPx / fontSizePx).toInt() + 4
        when (fadeLength) {
            0 -> (fullScreen * 0.22f).toInt().coerceAtLeast(6)
            2 -> fullScreen + 3
            else -> (fullScreen * 0.55f).toInt().coerceAtLeast(10)
        }
    }

    // ── Discrete step timing ──────────────────────────────────────────────────
    // The physics loop runs at ~60 Hz. Each column steps exactly one character
    // row downward every N ticks — an instant jump with no sub-pixel movement.
    // This is the authentic Matrix look: no smooth scrolling, just discrete steps.
    //   0 (slow)   → step every 8 ticks  → ~7.5 rows/sec at 60 Hz
    //   1 (medium) → step every 4 ticks  → ~15 rows/sec at 60 Hz
    //   2 (fast)   → step every 2 ticks  → ~30 rows/sec at 60 Hz
    val baseTicksPerStep: Int = remember(speedLevel) {
        when (speedLevel) {
            0 -> 8
            2 -> 2
            else -> 4
        }
    }

    // ── Build column array ────────────────────────────────────────────────────
    // headRow is in discrete row units; Y = headRow * fontSizePx.
    // Columns independently vary their ticks-per-step by ±1 so they step at
    // slightly different rates, preserving the independent-column feel.
    val columns: Array<MatrixColumn> = remember(
        screenWidthPx, screenHeightPx, columnWidth, trailLength, baseTicksPerStep, fontSizePx
    ) {
        val rng = kotlin.random.Random
        val count = ((screenWidthPx / columnWidth).toInt() + 1).coerceAtLeast(1)
        val rowsOnScreen = (screenHeightPx / fontSizePx).toInt() + 2
        Array(count) { i ->
            val xPos = i * columnWidth + columnWidth * 0.5f
            // Stagger initial heads above screen so rain fills in gradually
            val startRow = -(rng.nextFloat() * rowsOnScreen * 1.5f).toInt()
            val slots = trailLength + rng.nextInt((trailLength / 4).coerceAtLeast(1))
            // ── 3-D depth ─────────────────────────────────────────────────────
            // Distribute columns across three depth layers with weighted probability:
            //   far (0.15–0.40)  ~30% of columns — small, slow, dim
            //   mid (0.45–0.70)  ~40% of columns — medium
            //   near (0.75–1.00) ~30% of columns — large, fast, bright
            val depth: Float = when (rng.nextInt(10)) {
                in 0..2 -> 0.15f + rng.nextFloat() * 0.25f  // far
                in 3..6 -> 0.45f + rng.nextFloat() * 0.25f  // mid
                else -> 0.75f + rng.nextFloat() * 0.25f  // near
            }
            // Far columns step less often (slower); near columns step more often (faster).
            // Extra ticks added = up to +6 for the farthest columns.
            val depthTickBonus = ((1f - depth) * 6f).toInt()
            val colTicks = (baseTicksPerStep + depthTickBonus + rng.nextInt(2)).coerceAtLeast(1)
            MatrixColumn(
                x = xPos,
                headRow = startRow,
                ticksPerStep = colTicks,
                chars = CharArray(slots) { randomMatrixChar() },
                trailSlots = slots,
                shuffleEvery = 2 + rng.nextInt(6),
                tickAccum = rng.nextInt(colTicks),  // stagger so not all step on tick 0
                depth = depth
            )
        }
    }

    // ── Enable/disable fade ───────────────────────────────────────────────────
    val overlayAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = MotionTokens.SpeedUtil.durationMs(700, animSpeed), easing = FastOutSlowInEasing),
        label = "matrix_alpha"
    )
    val motionFactor by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(durationMillis = MotionTokens.SpeedUtil.durationMs(850, animSpeed), easing = FastOutSlowInEasing),
        label = "matrix_motion_factor"
    )

    // ── Frame counter — establishes Compose snapshot dependency on Canvas ─────
    // IMPORTANT: declared before the early-return so the render loop can start
    // on the very first composition and drive the alpha animation forward.
    var frameCount by remember { mutableLongStateOf(0L) }

    LaunchedEffect(enabled, columns, screenHeightPx, fontSizePx) {
        if (enabled) {
            val rng = kotlin.random.Random
            val rowsOnScreen = (screenHeightPx / fontSizePx).toInt() + 2
            for (col in columns) {
                col.headRow = -(rng.nextFloat() * rowsOnScreen * 1.5f).toInt()
                col.tickAccum = rng.nextInt(col.ticksPerStep.coerceAtLeast(1))
                col.charAge = 0
                for (k in col.chars.indices) col.chars[k] = randomMatrixChar()
            }
        }
    }

    // ── Physics tick rate ─────────────────────────────────────────────────────
    // The physics loop runs at a fixed ~60 Hz regardless of display refresh rate.
    // Columns step by whole rows on their own tick counters, so display Hz is
    // irrelevant — render only fires when at least one column has stepped.
    val physicsHz: Long = 60L
    val physicsTargetNs: Long = 1_000_000_000L / physicsHz

    // ── Physics loop — Dispatchers.Default (background thread) ───────────────
    //
    // Each tick: increment each column's tickAccum. When it reaches ticksPerStep,
    // reset it to 0 and advance headRow by 1. This is a discrete row-step — the
    // character grid jumps exactly one row, no sub-pixel movement ever.
    // Zero allocations inside the loop — only integer arithmetic.
    // IMPORTANT: this LaunchedEffect must live before the early-return so that
    // the physics loop is already running when the alpha animation completes.
    LaunchedEffect(columns, enabled, physicsHz, motionFactor) {
        if (!enabled && overlayAlpha < 0.005f && motionFactor < 0.005f) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val targetNs = physicsTargetNs
            var lastTick = System.nanoTime()
            while (isActive && (enabled || overlayAlpha > 0.005f || motionFactor > 0.005f)) {
                val now = System.nanoTime()
                val elapsed = now - lastTick
                if (elapsed >= targetNs) {
                    lastTick = now
                    val rowsOnScreen = (screenHeightPx / fontSizePx).toInt() + 1
                    val speedDivisor = 1f + (1f - motionFactor) * 10f
                    for (col in columns) {
                        col.tickAccum++
                        val effectiveTicksPerStep = (col.ticksPerStep * speedDivisor).toInt().coerceAtLeast(1)
                        if (col.tickAccum >= effectiveTicksPerStep) {
                            col.tickAccum = 0
                            col.headRow++
                            if (col.headRow - col.trailSlots > rowsOnScreen) {
                                col.headRow = -(kotlin.random.Random.nextFloat() * rowsOnScreen * 0.7f).toInt()
                                for (k in col.chars.indices) col.chars[k] = randomMatrixChar()
                            }
                        }
                        if (motionFactor > 0.02f) {
                            col.charAge++
                            val effectiveShuffleEvery = (col.shuffleEvery * speedDivisor).toInt().coerceAtLeast(1)
                            if (col.charAge >= effectiveShuffleEvery) {
                                col.charAge = 0
                                val sz = col.chars.size
                                col.chars[kotlin.random.Random.nextInt(sz)] = randomMatrixChar()
                                if (sz > 4) {
                                    col.chars[kotlin.random.Random.nextInt(sz)] = randomMatrixChar()
                                }
                            }
                        }
                    }
                } else {
                    delay(((targetNs - elapsed) / 1_000_000L).coerceAtLeast(1L))
                }
            }
        }
    }

    // ── Render trigger — main thread, physics-rate aligned ───────────────────
    // Columns only change state once per physics tick, so we render at that
    // rate too — no point firing the Canvas faster than the data changes.
    // withFrameNanos gates us to vsync boundaries; the elapsed check skips
    // frames where nothing has changed (i.e. most frames at high refresh rates).
    // IMPORTANT: also before the early-return for the same reason as the physics loop.
    LaunchedEffect(enabled, physicsTargetNs, overlayAlpha, motionFactor) {
        if (!enabled && overlayAlpha < 0.005f && motionFactor < 0.005f) return@LaunchedEffect
        var lastRender = 0L
        while (isActive && (enabled || overlayAlpha > 0.005f || motionFactor > 0.005f)) {
            withFrameNanos { ns ->
                if (ns - lastRender >= physicsTargetNs) {
                    lastRender = ns
                    frameCount++
                }
            }
        }
    }

    // Skip drawing when fully invisible (alpha animation not yet started or faded out).
    // The LaunchedEffects above must remain before this guard so they start immediately.
    if (overlayAlpha < 0.005f) return

    // ── Pre-allocated native Paints — reused every frame, ZERO allocation ────
    val nativePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    // Bloom paint — previously had a BlurMaskFilter which forces software rasterization
    // (CPU Gaussian convolution) for every blurred drawText() call.
    // At medium density, ~30–50 blurred chars/frame × 60fps = thousands of CPU ops/sec.
    // Replaced: same paint without any maskFilter.  We fake the bloom by drawing each
    // head/hot-zone character 3× at increasing font sizes and decreasing alphas — pure
    // GPU alpha compositing, zero software blur path.
    val bloomPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
            // maskFilter intentionally NOT set — no BlurMaskFilter
        }
    }

    // Pre-compute ARGB ints so Color.toArgb() is never called inside the draw loop
    val headArgb = remember(renderedHeadColor) { renderedHeadColor.toArgb() }
    val rainArgb = remember(renderedRainColor) { renderedRainColor.toArgb() }
    val trailArgb = remember(renderedTrailColor) { renderedTrailColor.toArgb() }
    val bgArgb = remember(backgroundColor, backgroundAlpha) {
        android.graphics.Color.argb(
            (backgroundAlpha * 255f).toInt().coerceIn(0, 255),
            android.graphics.Color.red(backgroundColor.toArgb()),
            android.graphics.Color.green(backgroundColor.toArgb()),
            android.graphics.Color.blue(backgroundColor.toArgb())
        )
    }

    // ── Trail colour LUT ─────────────────────────────────────────────────────
    // The hot-zone → tail colour lerp (rRain→rTrail, gRain→gTrail, bRain→bTrail)
    // was previously computed per-character per-frame inside the draw loop —
    // three float multiplications + three toInt() calls × up to ~500 characters
    // per frame = tens of thousands of operations/sec at high density.
    //
    // Precompute 64 evenly-spaced RGB steps covering t ∈ [0,1].  At draw time
    // map t → index with one multiply+cast, then read packed RGB from the LUT.
    // The LUT is rebuilt only when rain/trail colours change (never at runtime
    // while ACCENT COLOR eases).
    //
    // 64 steps → max colour error < 2/255 per channel — visually perfect.
    val trailRgbLut: IntArray = remember(rainArgb, trailArgb) {
        val rR = android.graphics.Color.red(rainArgb)
        val gR = android.graphics.Color.green(rainArgb)
        val bR = android.graphics.Color.blue(rainArgb)
        val rT = android.graphics.Color.red(trailArgb)
        val gT = android.graphics.Color.green(trailArgb)
        val bT = android.graphics.Color.blue(trailArgb)
        IntArray(TRAIL_LUT_SIZE) { i ->
            val fade = i.toFloat() / (TRAIL_LUT_SIZE - 1)
            val fi = 1f - fade
            val r = (rR * fi + rT * fade).toInt().coerceIn(0, 255)
            val g = (gR * fi + gT * fade).toInt().coerceIn(0, 255)
            val b = (bR * fi + bT * fade).toInt().coerceIn(0, 255)
            (r shl 16) or (g shl 8) or b  // packed 0x00RRGGBB
        }
    }

    // ── Canvas ────────────────────────────────────────────────────────────────
    val currentFrame = frameCount

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = overlayAlpha)
    ) {
        @Suppress("UNUSED_EXPRESSION") currentFrame
        // drawContext.canvas.nativeCanvas — stays hardware-accelerated.
        // drawIntoCanvas { } here would force software rendering for the entire
        // matrix Canvas on every frame, same root cause as the star mode lag.
        val nc = drawContext.canvas.nativeCanvas

        // Optional solid background
        if (backgroundAlpha > 0.001f) {
            nc.drawColor(bgArgb)
        }

        // rHead/gHead/bHead and rRain/gRain/bRain/rTrail/gTrail/bTrail previously
        // decomposed here for per-character lerp. Now headArgb/rainArgb are set
        // directly as paint.color, and the trail lerp is replaced by trailRgbLut.
        // These per-frame Color.red/green/blue decompositions are no longer needed.
        val baseA = overlayAlpha

        // ── Bloom pass then sharp pass ────────────────────────────────────
        // We render all columns twice:
        //   Pass 1 (bloom): head + hot-zone only, large blur, low alpha → glow halo
        //   Pass 2 (sharp): all characters at full crispness on top
        // Drawing bloom first means the sharp characters always render over the glow.
        //
        // Bloom radius scales with fontSizePx so it looks right at all glyph sizes.
        // Only the head and the first few hot-zone slots get bloom — the tail does not,
        // keeping performance budget low (< 10% of columns visible at any time are
        // head/hot-zone) and preserving visual hierarchy.

        // bloomRadius previously drove the BlurMaskFilter; removed with the blur pass.

        // ── PASS 1: Fake bloom glow (no BlurMaskFilter) ───────────────────
        // Draw each head/hot-zone character 3× at different scales + alphas.
        // Largest scale = outermost halo; smallest = tight inner ring.
        // All compositing happens on the GPU (alpha blending); zero software path.
        for (col in columns) {
            val depth = col.depth
            val scaledSize = fontSizePx * (0.45f + depth * 0.55f)
            val depthAlpha = 0.25f + depth * 0.75f

            val headY = col.headRow * scaledSize
            val slots = col.chars.size

            for (slot in 0..minOf(2, slots - 1)) {
                val charY = headY - slot * scaledSize
                if (charY < -scaledSize || charY > size.height + scaledSize) continue

                // Per-slot base alpha for bloom (strong at head, falls off fast)
                val baseBloom = when (slot) {
                    0 -> 0.50f
                    1 -> 0.28f
                    else -> 0.14f
                } * baseA * depthAlpha

                val bArgb = if (slot == 0) headArgb else rainArgb

                // Layer 1 — outermost (1.22× size, most transparent)
                val alpha1 = (baseBloom * 0.35f).coerceIn(0f, 1f)
                if (alpha1 > 0.01f) {
                    bloomPaint.textSize = scaledSize * 1.22f
                    bloomPaint.color = bArgb
                    bloomPaint.alpha = (alpha1 * 255f).toInt()
                    nc.drawText(col.chars, slot % slots, 1, col.x, charY, bloomPaint)
                }
                // Layer 2 — mid (1.10× size)
                val alpha2 = (baseBloom * 0.55f).coerceIn(0f, 1f)
                if (alpha2 > 0.01f) {
                    bloomPaint.textSize = scaledSize * 1.10f
                    bloomPaint.color = bArgb
                    bloomPaint.alpha = (alpha2 * 255f).toInt()
                    nc.drawText(col.chars, slot % slots, 1, col.x, charY, bloomPaint)
                }
                // Layer 3 — tight inner bloom (1.03× size)
                val alpha3 = (baseBloom * 0.80f).coerceIn(0f, 1f)
                if (alpha3 > 0.01f) {
                    bloomPaint.textSize = scaledSize * 1.03f
                    bloomPaint.color = bArgb
                    bloomPaint.alpha = (alpha3 * 255f).toInt()
                    nc.drawText(col.chars, slot % slots, 1, col.x, charY, bloomPaint)
                }
            }
        }

        // ── PASS 2: Sharp characters ──────────────────────────────────────
        nativePaint.maskFilter = null  // ensure sharp (no blur)
        for (col in columns) {
            val depth = col.depth
            val scaledSize = fontSizePx * (0.45f + depth * 0.55f)
            val depthAlpha = 0.25f + depth * 0.75f

            nativePaint.textSize = scaledSize

            val headY = col.headRow * scaledSize
            val slots = col.chars.size
            val slotsM = (slots - 1).coerceAtLeast(1)

            for (slot in 0 until slots) {
                val charY = headY - slot * scaledSize
                if (charY < -scaledSize || charY > size.height + scaledSize) continue

                val t = slot.toFloat() / slotsM   // 0 = head, 1 = tail

                val alpha: Int
                val argb: Int

                when {
                    slot == 0 -> {
                        alpha = (baseA * depthAlpha * 255f).toInt().coerceIn(0, 255)
                        argb = headArgb
                    }

                    slot <= 3 -> {
                        alpha = (baseA * depthAlpha * 240f).toInt().coerceIn(0, 255)
                        argb = rainArgb
                    }

                    else -> {
                        val fade = ((t - 0.12f) / 0.88f).coerceIn(0f, 1f)
                        // LUT lookup: one int cast instead of 3 float multiplies + 3 toInt() calls.
                        // Map fade [0,1] → LUT index [0, TRAIL_LUT_SIZE-1], read packed 0x00RRGGBB.
                        val lutIdx = (fade * (TRAIL_LUT_SIZE - 1) + 0.5f).toInt()
                            .coerceIn(0, TRAIL_LUT_SIZE - 1)
                        alpha = (baseA * depthAlpha * (1f - fade * fade) * 210f).toInt().coerceIn(0, 255)
                        argb =
                            trailRgbLut[lutIdx] or 0xFF000000.toInt()  // OR in opaque sentinel; alpha set via paint.alpha below
                    }
                }

                if (alpha < 3) continue

                nativePaint.color = argb
                nativePaint.alpha = alpha
                // CharArray overload: zero String allocation per character
                nc.drawText(col.chars, slot % slots, 1, col.x, charY, nativePaint)
            }
        }
    }
}

// CompositionLocals
