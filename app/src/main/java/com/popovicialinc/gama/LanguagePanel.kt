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
fun LanguagePanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    isSmallScreen: Boolean,
    isLandscape: Boolean,
    isTablet: Boolean,
    colors: ThemeColors,
    cardBackground: Color,
    performHaptic: () -> Unit,
    oledMode: Boolean
) {
    val ts      = LocalTypeScale.current
    val context = LocalContext.current
    val view    = LocalView.current
    val prefs   = remember { context.getSharedPreferences("gama_prefs", Context.MODE_PRIVATE) }
    val strings = LocalStrings.current

    // Read + write the shared state owned by GamaLocalizationProvider —
    // updating this triggers an immediate strings reload with no restart needed.
    val languageCodeState = LocalLanguageCode.current
    val selectedCode by languageCodeState

    var availableLanguages by remember { mutableStateOf<List<LanguageEntry>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Reload the language list every time the panel opens.
    // Important: do NOT clear the list immediately when the panel starts closing.
    // The close animation still measures this content for a moment; clearing it on
    // dismiss made the language list vanish instantly, causing the whole panel to
    // jump downward before the exit transition finished.
    LaunchedEffect(visible) {
        if (visible) {
            searchQuery = ""
            availableLanguages = LocalizationManager.loadAvailableLanguages(context)
        }
    }

    val animSpeed = LocalAnimationSpeed.current

    val filtered = remember(availableLanguages, searchQuery) {
        if (searchQuery.isBlank()) availableLanguages
        else availableLanguages.filter { lang ->
            lang.name.contains(searchQuery, ignoreCase = true) ||
            lang.nativeName.contains(searchQuery, ignoreCase = true) ||
            lang.code.contains(searchQuery, ignoreCase = true)
        }
    }

    PanelScaffold(
        visible       = visible,
        onDismiss     = onDismiss,
        isLandscape   = isLandscape,
        isSmallScreen = isSmallScreen,
        oledMode      = oledMode,
        colors        = colors
    ) { _ ->

        CleanTitle(
            text         = strings["language_panel.title"].ifEmpty { "LANGUAGE" },
            fontSize     = if (isLandscape) ts.displayMedium else ts.displayLarge,
            colors       = colors,
            scrollOffset = 0
        )

        // ── Search bar ─────────────────────────────────────────────────────
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 1, totalItems = 4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty())
                            colors.primaryAccent.copy(alpha = 0.7f)
                        else
                            colors.border.copy(alpha = if (oledMode) 0.4f else 0.25f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = cardBackground),
                    shape    = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text       = "⌕",
                            fontSize   = 24.sp,
                            color      = colors.textSecondary.copy(alpha = 0.58f),
                            fontFamily = quicksandFontFamily
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text       = strings["language_panel.search_placeholder"].ifEmpty { "Search languages…" },
                                    fontSize   = ts.bodyMedium,
                                    color      = colors.textSecondary.copy(alpha = 0.35f),
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            BasicTextField(
                                value         = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine    = true,
                                textStyle     = TextStyle(
                                    fontSize   = ts.bodyMedium,
                                    color      = colors.textPrimary,
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush   = SolidColor(colors.primaryAccent),
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter   = fadeIn(tween(MotionTokens.SpeedUtil.durationMs(160, animSpeed), easing = MotionTokens.Easing.enter)) + scaleIn(
                                animationSpec = tween(MotionTokens.SpeedUtil.durationMs(160, animSpeed), easing = MotionTokens.Easing.enter),
                                initialScale = 0.72f
                            ),
                            exit    = fadeOut(tween(MotionTokens.SpeedUtil.durationMs(120, animSpeed), easing = MotionTokens.Easing.exit)) + scaleOut(
                                animationSpec = tween(MotionTokens.SpeedUtil.durationMs(120, animSpeed), easing = MotionTokens.Easing.exit),
                                targetScale = 0.72f
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.textSecondary.copy(alpha = 0.12f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = "✕",
                                    fontSize   = 10.sp,
                                    color      = colors.textSecondary.copy(alpha = 0.6f),
                                    fontFamily = quicksandFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Language list ──────────────────────────────────────────────────
        AnimatedElement(visible = visible, cardShadow = true, staggerIndex = 2, totalItems = 4) {
            if (filtered.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = strings["language_panel.no_results"].ifEmpty { "No languages match \"%s\"" }.replace("%s", searchQuery),
                        fontSize   = ts.bodyMedium,
                        color      = colors.textSecondary.copy(alpha = 0.4f),
                        fontFamily = quicksandFontFamily,
                        fontWeight = FontWeight.Medium,
                        textAlign  = TextAlign.Center
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = colors.primaryAccent.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(28.dp))
                ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = cardBackground),
                        shape    = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            filtered.forEachIndexed { index, lang ->
                                LanguageRow(
                                    lang       = lang,
                                    isSelected = lang.code == selectedCode,
                                    isLast     = index == filtered.lastIndex,
                                    colors     = colors,
                                    oledMode   = oledMode,
                                    onClick    = {
                                        if (lang.code != selectedCode) {
                                            GamaHaptics.languageChanged(context, view)
                                            // Write to the shared state — provider reacts immediately
                                            languageCodeState.value = lang.code
                                            // Persist to prefs so it survives restarts
                                            LocalizationManager.saveCode(prefs, lang.code)
                                            LocalizationManager.invalidateCache()
                                        } else {
                                            performHaptic()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}

@Composable
internal fun LanguageRow(
    lang: LanguageEntry,
    isSelected: Boolean,
    isLast: Boolean,
    colors: ThemeColors,
    oledMode: Boolean,
    onClick: () -> Unit
) {
    val ts = LocalTypeScale.current
    val strings = LocalStrings.current
    val context = LocalContext.current
    val view = LocalView.current

    val animSpeed = LocalAnimationSpeed.current
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed) MotionTokens.Scale.subtle else 1f,
        animationSpec = spring(
            dampingRatio = if (isPressed) MotionTokens.Springs.pressDown.dampingRatio
                           else MotionTokens.Springs.pressUp.dampingRatio,
            stiffness    = if (isPressed) MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.pressDown.stiffness, animSpeed)
                           else MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.pressUp.stiffness, animSpeed)
        ),
        label = "lang_row_press"
    )

    val accentBarAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(MotionTokens.SpeedUtil.durationMs(220, animSpeed), easing = MotionTokens.Easing.enter),
        label         = "accent_bar_alpha"
    )
    val accentBarHeight by animateDpAsState(
        targetValue   = if (isSelected) 28.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = MotionTokens.Springs.snappy.dampingRatio,
            stiffness    = MotionTokens.SpeedUtil.stiffness(MotionTokens.Springs.snappy.stiffness, animSpeed)
        ),
        label         = "accent_bar_height"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val hapticStartedAt = GamaHaptics.pressStart(context, view)
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        GamaHaptics.releaseAfterPress(context, view, hapticStartedAt, released)
                        if (released) onClick()
                    }
                )
            }
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(accentBarHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primaryAccent.copy(alpha = accentBarAlpha))
            )

            // Flag
            Text(
                text       = lang.flag,
                fontSize   = if (isSelected) 22.sp else 20.sp,
                modifier   = Modifier.width(28.dp),
                textAlign  = TextAlign.Center
            )

            // Names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = lang.nativeName,
                    fontSize   = ts.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color      = if (isSelected) colors.primaryAccent else colors.textPrimary,
                    fontFamily = quicksandFontFamily
                )
                if (lang.nativeName != lang.name) {
                    Text(
                        text       = lang.name,
                        fontSize   = ts.bodySmall,
                        color      = colors.textSecondary.copy(alpha = 0.55f),
                        fontFamily = quicksandFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // "Active" badge
            AnimatedVisibility(
                visible = isSelected,
                enter   = fadeIn(tween(MotionTokens.SpeedUtil.durationMs(200, animSpeed), easing = MotionTokens.Easing.enter)) +
                          slideInVertically(tween(MotionTokens.SpeedUtil.durationMs(200, animSpeed), easing = MotionTokens.Easing.emphasizedDecelerate)) { -it / 3 },
                exit    = fadeOut(tween(MotionTokens.SpeedUtil.durationMs(130, animSpeed), easing = MotionTokens.Easing.exit)) +
                          slideOutVertically(tween(MotionTokens.SpeedUtil.durationMs(130, animSpeed), easing = MotionTokens.Easing.exit)) { -it / 4 }
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primaryAccent.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            colors.primaryAccent.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text          = strings["language_panel.active_badge"].ifEmpty { "Active" },
                        fontSize      = ts.labelSmall,
                        color         = colors.primaryAccent,
                        fontFamily    = quicksandFontFamily,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Divider (skip on last row)
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 20.dp)
                    .height(0.5.dp)
                    .background(colors.border.copy(alpha = 0.12f))
            )
        }
    }
}

