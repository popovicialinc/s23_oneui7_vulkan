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
fun TitleSection(
    colors: ThemeColors,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    userName: String = "",
    currentHour: Int = 12,
    onEasterEgg: (() -> Unit)? = null
) {
    val ts = LocalTypeScale.current

    val strings = LocalStrings.current
    val languageCode = LocalLanguageCode.current.value

    // ── Greeting pools per time period ───────────────────────────────────────
    // Day greetings: warm, energetic, optimistic
    // Night greetings: introspective, calm, thoughtful
    val greetingPool: List<String> = when (currentHour) {
        in 0..5 -> if (userName.isNotEmpty()) listOf(
            strings["greetings.late_night_named_1"].replace("%s", userName)
                .ifEmpty { "The night is yours, $userName. 🌙" },
            strings["greetings.late_night_named_2"].replace("%s", userName)
                .ifEmpty { "Some thoughts only come after midnight, $userName. 🌙" },
            strings["greetings.late_night_named_3"].replace("%s", userName)
                .ifEmpty { "Even the stars are listening, $userName. 🌙" },
            strings["greetings.late_night_named_4"].replace("%s", userName)
                .ifEmpty { "The world narrows to this moment, $userName. 🌙" },
            strings["greetings.late_night_named_5"].replace("%s", userName)
                .ifEmpty { "There's a particular clarity to these hours, $userName. 🌙" }
        ) else listOf(
            strings["greetings.late_night_1"].ifEmpty { "The night is yours. 🌙" },
            strings["greetings.late_night_2"].ifEmpty { "Some thoughts only come after midnight. 🌙" },
            strings["greetings.late_night_3"].ifEmpty { "Even the stars are listening. 🌙" },
            strings["greetings.late_night_4"].ifEmpty { "The world narrows to this moment. 🌙" },
            strings["greetings.late_night_5"].ifEmpty { "There's a particular clarity to these hours. 🌙" }
        )

        in 6..11 -> if (userName.isNotEmpty()) listOf(
            strings["greetings.morning_named_1"].replace("%s", userName)
                .ifEmpty { "Good morning, $userName! ☀️ Make it a productive one." },
            strings["greetings.morning_named_2"].replace("%s", userName)
                .ifEmpty { "Up and at it, $userName! ☀️ The day has potential." },
            strings["greetings.morning_named_3"].replace("%s", userName)
                .ifEmpty { "A brand new day, $userName! ☀️ Anything is possible." },
            strings["greetings.morning_named_4"].replace("%s", userName)
                .ifEmpty { "Morning energy, $userName! ☀️ The best kind." },
            strings["greetings.morning_named_5"].replace("%s", userName)
                .ifEmpty { "The day is yours, $userName! ☀️ Go and take it." }
        ) else listOf(
            strings["greetings.morning_1"].ifEmpty { "Good morning! ☀️ Make it a productive one." },
            strings["greetings.morning_2"].ifEmpty { "Up and at it! ☀️ The day has potential." },
            strings["greetings.morning_3"].ifEmpty { "A brand new day! ☀️ Anything is possible." },
            strings["greetings.morning_4"].ifEmpty { "Morning energy! ☀️ The best kind." },
            strings["greetings.morning_5"].ifEmpty { "The day is yours! ☀️ Go and take it." }
        )

        in 12..16 -> if (userName.isNotEmpty()) listOf(
            strings["greetings.afternoon_named_1"].replace("%s", userName)
                .ifEmpty { "Hey, $userName! ☀️ That afternoon rhythm is real." },
            strings["greetings.afternoon_named_2"].replace("%s", userName)
                .ifEmpty { "Good afternoon, $userName! ☀️ Keep the energy up." },
            strings["greetings.afternoon_named_3"].replace("%s", userName).ifEmpty { "Keep it going, $userName! ☀️" },
            strings["greetings.afternoon_named_4"].replace("%s", userName)
                .ifEmpty { "Halfway through, $userName! ☀️ Finish what you started." },
            strings["greetings.afternoon_named_5"].replace("%s", userName)
                .ifEmpty { "There you are, $userName! ☀️ Let's do something great." }
        ) else listOf(
            strings["greetings.afternoon_1"].ifEmpty { "Hey! ☀️ That afternoon rhythm is real." },
            strings["greetings.afternoon_2"].ifEmpty { "Good afternoon! ☀️ Keep the energy up." },
            strings["greetings.afternoon_3"].ifEmpty { "Keep it going! ☀️" },
            strings["greetings.afternoon_4"].ifEmpty { "Halfway through! ☀️ Finish what you started." },
            strings["greetings.afternoon_5"].ifEmpty { "There you are! ☀️ Let's do something great." }
        )

        in 17..22 -> if (userName.isNotEmpty()) listOf(
            strings["greetings.evening_named_1"].replace("%s", userName)
                .ifEmpty { "Evening is settling in, $userName. 🌙" },
            strings["greetings.evening_named_2"].replace("%s", userName).ifEmpty { "A quieter pace now, $userName. 🌙" },
            strings["greetings.evening_named_3"].replace("%s", userName)
                .ifEmpty { "The day is winding down — worth reflecting on, $userName. 🌙" },
            strings["greetings.evening_named_4"].replace("%s", userName)
                .ifEmpty { "Unwinding has its own kind of beauty, $userName. 🌙" },
            strings["greetings.evening_named_5"].replace("%s", userName)
                .ifEmpty { "The night is gentle tonight, $userName. 🌙" }
        ) else listOf(
            strings["greetings.evening_1"].ifEmpty { "Evening is settling in. 🌙" },
            strings["greetings.evening_2"].ifEmpty { "A quieter pace now. 🌙" },
            strings["greetings.evening_3"].ifEmpty { "The day is winding down — worth reflecting on. 🌙" },
            strings["greetings.evening_4"].ifEmpty { "Unwinding has its own kind of beauty. 🌙" },
            strings["greetings.evening_5"].ifEmpty { "The night is gentle tonight. 🌙" }
        )

        else -> if (userName.isNotEmpty()) listOf(
            strings["greetings.late_night_named_1"].replace("%s", userName)
                .ifEmpty { "The night is yours, $userName. 🌙" },
            strings["greetings.late_night_named_2"].replace("%s", userName)
                .ifEmpty { "Some thoughts only come after midnight, $userName. 🌙" },
            strings["greetings.late_night_named_5"].replace("%s", userName)
                .ifEmpty { "There's a particular clarity to these hours, $userName. 🌙" },
            strings["greetings.late_night_named_3"].replace("%s", userName)
                .ifEmpty { "Even the stars are listening, $userName. 🌙" },
            strings["greetings.late_night_named_4"].replace("%s", userName)
                .ifEmpty { "The world narrows to this moment, $userName. 🌙" }
        ) else listOf(
            strings["greetings.late_night_1"].ifEmpty { "The night is yours. 🌙" },
            strings["greetings.late_night_2"].ifEmpty { "Some thoughts only come after midnight. 🌙" },
            strings["greetings.late_night_5"].ifEmpty { "There's a particular clarity to these hours. 🌙" },
            strings["greetings.late_night_3"].ifEmpty { "Even the stars are listening. 🌙" },
            strings["greetings.late_night_4"].ifEmpty { "The world narrows to this moment. 🌙" }
        )
    }
    val displayText = remember(currentHour, userName, strings) { greetingPool.random().withoutGreetingEmoji() }

    val configuration = LocalConfiguration.current
    val isLandscapeTitle = configuration.screenWidthDp > configuration.screenHeightDp
    val screenMinDpTitle = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val titleHPad = (screenMinDpTitle * 0.06f).dp.coerceIn(16.dp, 32.dp)
    val titleVPad = when {
        isSmallScreen -> 8.dp
        isLandscapeTitle -> (screenMinDpTitle * 0.020f).dp.coerceIn(6.dp, 12.dp)
        else -> (screenMinDpTitle * 0.038f).dp.coerceIn(12.dp, 22.dp)
    }
    val titleItemSpacing = when {
        isSmallScreen -> 8.dp
        isLandscapeTitle -> (screenMinDpTitle * 0.022f).dp.coerceIn(7.dp, 12.dp)
        else -> (screenMinDpTitle * 0.032f).dp.coerceIn(10.dp, 18.dp)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(titleItemSpacing),
        modifier = Modifier
            .padding(horizontal = titleHPad, vertical = titleVPad)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = MotionTokens.SpeedUtil.durationMs(300, LocalAnimationSpeed.current),
                    easing = MotionTokens.Easing.emphasizedDecelerate
                )
            )
    ) {
        // Use AnimatedContent for smooth text substitution if the greeting changes,
        // though typically it just appends the name. animateContentSize on parent handles the width change.
        Text(
            text = displayText,
            fontSize = ts.bodyLarge,
            fontFamily = quicksandFontFamily,
            color = colors.textSecondary.copy(alpha = 0.8f),
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold // Force Bold
        )

        val gamaTextSize = if (isLandscape) (ts.displayLarge.value * 1.15f).sp else (ts.displayLarge.value * 1.10f).sp
        val context = LocalContext.current
        val quicksandBoldTypeface = remember {
            try {
                android.graphics.Typeface.createFromAsset(context.assets, "fonts/quicksand_bold.ttf")
            } catch (e: Exception) {
                android.graphics.Typeface.DEFAULT_BOLD
            }
        }

        // One-shot shimmer matching CleanTitle — fires once on composition, then settles permanently
        val animationLevel = LocalAnimationLevel.current
        val animSpeed = LocalAnimationSpeed.current
        var shimmerTarget by remember { mutableStateOf(0f) }
        val shimmerProgress by animateFloatAsState(
            targetValue = shimmerTarget,
            animationSpec = if (animationLevel == 2) snap()
            else tween(durationMillis = MotionTokens.SpeedUtil.durationMs(900, animSpeed), easing = MotionTokens.Easing.silk),
            label = "gama_title_shimmer"
        )
        LaunchedEffect(Unit) {
            if (animationLevel != 2) delay(250)
            shimmerTarget = 1f
        }

        // ── Fake-glow + chromatic-aberration paints ───────────────────────────
        // Previously used BlurMaskFilter(80f) — a software CPU gaussian that runs
        // on every draw invalidation.  Replaced with 3 alpha layers + CA offsets:
        //   • ZERO BlurMaskFilter / ShadowLayer calls — purely GPU compositing.
        //   • 5 extra drawText() calls per frame vs 1 blurred call — net faster.
        //   • CA shifts R left, B right — gives the title its cyberpunk split-prism look.
        val titleGlowBase = remember(colors.textPrimary, quicksandBoldTypeface) {
            android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = quicksandBoldTypeface
            }
        }
        // Pre-compute accent ARGB so no Color.toArgb() allocations inside draw
        val glowArgb = remember(colors.textPrimary) { colors.textPrimary.copy(alpha = 1f).toArgb() }
        val caRedArgb = remember { android.graphics.Color.argb(200, 255, 60, 60) }
        val caBluArgb = remember { android.graphics.Color.argb(200, 60, 100, 255) }

        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left bar — shimmer sweeps left → right (toward text)
                Box(
                    modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .drawWithContent {
                        drawContent()
                        // Only draw shimmer band while the animation is in flight
                        if (shimmerProgress > 0f && shimmerProgress < 1f) {
                            val bandWidth = size.width * 0.35f
                            val bandCenter = shimmerProgress * (size.width + bandWidth) - bandWidth * 0.5f
                            val shimmerBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.primaryAccent.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.75f),
                                    colors.primaryAccent.copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                startX = bandCenter - bandWidth * 0.5f,
                                endX = bandCenter + bandWidth * 0.5f
                            )
                            drawRect(brush = shimmerBrush)
                        }
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.primaryAccent.copy(alpha = 0f),
                                colors.primaryAccent.copy(alpha = 1f)
                            )
                        )
                    ))
                Text(
                    text = "GAMA", fontSize = gamaTextSize, fontWeight = FontWeight.Bold,
                    fontFamily = quicksandFontFamily, color = colors.textPrimary, textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 24.dp).pointerInput(onEasterEgg) {
                        if (onEasterEgg != null) detectTapGestures(onLongPress = { onEasterEgg() })
                    }.drawWithContent {
                        val baseSize = gamaTextSize.toPx()
                        val cx = size.width / 2f
                        val cy = size.height / 2f + baseSize * 0.25f
                        // Chromatic aberration offset — ~2% of glyph height
                        val caOff = baseSize * 0.040f
                        drawIntoCanvas { canvas ->
                            val nc = canvas.nativeCanvas
                            // ── Glow halos (3 layers, no blur — pure alpha compositing) ───
                            titleGlowBase.color = glowArgb
                            // Layer 3 — outermost halo (biggest, most transparent)
                            titleGlowBase.alpha = 18
                            titleGlowBase.textSize = baseSize * 1.13f
                            nc.drawText("GAMA", cx, cy, titleGlowBase)
                            // Layer 2
                            titleGlowBase.alpha = 35
                            titleGlowBase.textSize = baseSize * 1.06f
                            nc.drawText("GAMA", cx, cy, titleGlowBase)
                            // Layer 1 — tightest bloom
                            titleGlowBase.alpha = 60
                            titleGlowBase.textSize = baseSize * 1.02f
                            nc.drawText("GAMA", cx, cy, titleGlowBase)
                            // ── Chromatic aberration — R left, B right ────────────────
                            titleGlowBase.textSize = baseSize
                            // Red fringe
                            titleGlowBase.color = caRedArgb
                            titleGlowBase.alpha = android.graphics.Color.alpha(caRedArgb)
                            nc.drawText("GAMA", cx - caOff, cy, titleGlowBase)
                            // Blue fringe
                            titleGlowBase.color = caBluArgb
                            titleGlowBase.alpha = android.graphics.Color.alpha(caBluArgb)
                            nc.drawText("GAMA", cx + caOff, cy, titleGlowBase)
                        }
                        // Crisp white Compose text on top
                        drawContent()
                    })
                // Right bar — shimmer sweeps right → left (toward text)
                Box(
                    modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .drawWithContent {
                        drawContent()
                        if (shimmerProgress > 0f && shimmerProgress < 1f) {
                            val bandWidth = size.width * 0.35f
                            val bandCenter = (1f - shimmerProgress) * (size.width + bandWidth) - bandWidth * 0.5f
                            val shimmerBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.primaryAccent.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.75f),
                                    colors.primaryAccent.copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                startX = bandCenter - bandWidth * 0.5f,
                                endX = bandCenter + bandWidth * 0.5f
                            )
                            drawRect(brush = shimmerBrush)
                        }
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.primaryAccent.copy(alpha = 1f),
                                colors.primaryAccent.copy(alpha = 0f)
                            )
                        )
                    ))
                // end right bar
            }
        }

        // Standing by text - 50% bigger
        Text(
            text = LocalStrings.current["main.standing_by"].ifEmpty { "Standing by and awaiting your command" },
            fontSize = ts.bodyLarge, // greeting name
            fontFamily = quicksandFontFamily,
            color = colors.textSecondary,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold
        )

        // Additional spacer to increase space between standing by text and cards
        Spacer(modifier = Modifier.height((screenMinDpTitle * 0.03f).dp.coerceIn(12.dp, 20.dp)))

        // CHOOSE YOUR PATH. text in accent color - slightly bigger
        Text(
            text = LocalStrings.current["main.whats_next"].ifEmpty { "CHOOSE YOUR PATH." },
            fontSize = ts.headlineSmall, // slightly bigger than bodyLarge
            fontFamily = quicksandFontFamily,
            color = colors.primaryAccent.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold, // Force Bold
            letterSpacing = 2.sp,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun CleanTitle(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    colors: ThemeColors,
    reverseGradient: Boolean = false,
    scrollState: androidx.compose.foundation.ScrollState? = null
) {
    val panelScrollState = scrollState ?: LocalPanelScrollState.current
    val titleColor = colors.textPrimary
    val animationLevel = LocalAnimationLevel.current
    val animSpeed = LocalAnimationSpeed.current
    val context = LocalContext.current
    val quicksandBoldTypeface = remember {
        try {
            android.graphics.Typeface.createFromAsset(context.assets, "fonts/quicksand_bold.ttf")
        } catch (e: Exception) {
            android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    // One-shot shimmer: sweeps once on the very first app launch, then never again.
    // "title_shimmer_played" is written to SharedPreferences after the animation
    // completes, so re-entering the composable (panel close/reopen, recomposition)
    // finds the flag already set and skips the effect entirely.
    val prefs = remember { context.getSharedPreferences("gama_prefs", android.content.Context.MODE_PRIVATE) }
    val shimmerAlreadyPlayed = remember { prefs.getBoolean("title_shimmer_played", false) }
    // Start at 1f immediately if we should skip, so the bars render in their settled state
    var shimmerTarget by remember { mutableStateOf(if (shimmerAlreadyPlayed) 1f else 0f) }
    val shimmerProgress by animateFloatAsState(
        targetValue = shimmerTarget,
        animationSpec = if (shimmerAlreadyPlayed || animationLevel == 2) snap()
        else tween(durationMillis = MotionTokens.SpeedUtil.durationMs(900, animSpeed), easing = MotionTokens.Easing.silk),
        label = "title_shimmer"
    )
    // Kick off after a short delay so the panel entrance has started and the
    // shimmer reads as a follow-on flourish rather than fighting the open animation.
    // Skipped entirely on every run after the first.
    LaunchedEffect(Unit) {
        if (!shimmerAlreadyPlayed) {
            if (animationLevel != 2) delay(250)
            shimmerTarget = 1f
            // Persist immediately after triggering — we don't wait for the animation
            // to finish because we want the flag written even if the user navigates away
            prefs.edit().putBoolean("title_shimmer_played", true).apply()
        }
    }

    // FIXED GRADIENT BARS — always use horizontal gradient regardless of orientation.
    // scrollState.value is read INSIDE the graphicsLayer lambda (deferred read):
    // scrolling only re-draws the title layer instead of recomposing the whole
    // panel that hosts it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = -(panelScrollState?.value ?: 0) * 0.4f
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left Gradient Bar — fades transparent→accent, shimmer sweeps left→right
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .drawWithContent {
                    drawContent()
                    // Only allocate shimmer Brush while animation is in flight
                    if (shimmerProgress > 0f && shimmerProgress < 1f) {
                        val bandWidth = size.width * 0.35f
                        val bandCenter = shimmerProgress * (size.width + bandWidth) - bandWidth * 0.5f
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.primaryAccent.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.75f),
                                colors.primaryAccent.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startX = bandCenter - bandWidth * 0.5f,
                            endX = bandCenter + bandWidth * 0.5f
                        )
                        drawRect(brush = shimmerBrush)
                    }
                }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (reverseGradient)
                            listOf(colors.primaryAccent.copy(alpha = 1f), colors.primaryAccent.copy(alpha = 0f))
                        else
                            listOf(colors.primaryAccent.copy(alpha = 0f), colors.primaryAccent.copy(alpha = 1f))
                    )
                )
        )

        val lines = text.split("\n")
        if (lines.size <= 1) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = quicksandFontFamily,
                color = titleColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                lines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quicksandFontFamily,
                        color = titleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )
                }
            }
        }

        // Right Gradient Bar — fades accent→transparent, shimmer sweeps right→left
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .drawWithContent {
                    drawContent()
                    // Only allocate shimmer Brush while animation is in flight
                    if (shimmerProgress > 0f && shimmerProgress < 1f) {
                        val bandWidth = size.width * 0.35f
                        val bandCenter = (1f - shimmerProgress) * (size.width + bandWidth) - bandWidth * 0.5f
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.primaryAccent.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.75f),
                                colors.primaryAccent.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            startX = bandCenter - bandWidth * 0.5f,
                            endX = bandCenter + bandWidth * 0.5f
                        )
                        drawRect(brush = shimmerBrush)
                    }
                }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (reverseGradient)
                            listOf(colors.primaryAccent.copy(alpha = 0f), colors.primaryAccent.copy(alpha = 1f))
                        else
                            listOf(colors.primaryAccent.copy(alpha = 1f), colors.primaryAccent.copy(alpha = 0f))
                    )
                )
        )
    }
}
