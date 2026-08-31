package com.popovicialinc.gama.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * PreferencesManager handles all preference-related state and persistence for GAMA.
 *
 * This class encapsulates the shared preferences operations, providing a clean
 * separation of concerns by centralizing all preference storage and retrieval logic.
 */
class PreferencesManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "gama_prefs"
        private const val PREFS_VERSION = 4

        const val USER_NAME_KEY = "user_name"
        const val BUTTON_LABELS_SHOWN_KEY = "button_labels_shown"
        const val NOTIF_PERM_REQUESTED_KEY = "notif_perm_requested"
        const val EXCLUDED_APPS_KEY = "excluded_apps"
        const val PREFS_VERSION_KEY = "prefs_version"

        // Animation preferences
        const val ANIMATION_LEVEL_KEY = "animation_level"
        const val ANIMATION_SPEED_KEY = "animation_speed"
        const val GRADIENT_ENABLED_KEY = "gradient_enabled"

        // Particle preferences
        const val PARTICLES_ENABLED_KEY = "particles_enabled"
        const val PARTICLE_SPEED_KEY = "particle_speed"
        const val PARTICLE_PARALLAX_ENABLED_KEY = "particle_parallax_enabled"
        const val PARTICLE_PARALLAX_SENSITIVITY_KEY = "particle_parallax_sensitivity"
        const val PARTICLE_STAR_MODE_KEY = "particle_star_mode"
        const val PARTICLE_TIME_MODE_KEY = "particle_time_mode"
        const val TIME_OFFSET_HOURS_KEY = "time_offset_hours"
        const val PARTICLE_COUNT_KEY = "particle_count"
        const val PARTICLE_COUNT_CUSTOM_KEY = "particle_count_custom"

        // UI preferences
        const val BLUR_ENABLED_KEY = "blur_enabled"
        const val THEME_PREFERENCE_KEY = "theme_preference"
        const val USE_DYNAMIC_COLOR_KEY = "use_dynamic_color"
        const val ADVANCED_COLOR_PICKER_KEY = "advanced_color_picker"
        const val CUSTOM_ACCENT_KEY = "custom_accent"
        const val CUSTOM_GRADIENT_START_KEY = "custom_gradient_start"
        const val CUSTOM_GRADIENT_END_KEY = "custom_gradient_end"
        const val UI_SCALE_KEY = "ui_scale"

        // System preferences
        const val VERBOSE_MODE_KEY = "verbose_mode"
        const val AGGRESSIVE_MODE_KEY = "aggressive_mode"
        const val KILL_LAUNCHER_KEY = "kill_launcher"
        const val KILL_KEYBOARD_KEY = "kill_keyboard"
        const val SHOW_GPUWATCH_BUTTON_KEY = "show_gpuwatch_button"
        const val STAGGER_ENABLED_KEY = "stagger_enabled"
        const val BACK_BUTTON_AVOIDANCE_ENABLED_KEY = "back_button_avoidance_enabled"
        const val BACK_BUTTON_INVERSED_KEY = "back_button_inversed"
        const val SHADOWS_ENABLED_KEY = "shadows_enabled"

        // Haptic preferences
        const val HAPTICS_ENABLED_KEY = "haptics_enabled"
        const val HAPTICS_REGULAR_ENABLED_KEY = "haptics_regular_enabled"
        const val HAPTICS_HOLD_ENABLED_KEY = "haptics_hold_enabled"
        const val HAPTICS_RENDERER_ENABLED_KEY = "haptics_renderer_enabled"
        const val HAPTICS_LANGUAGE_ENABLED_KEY = "haptics_language_enabled"
        const val HAPTICS_BOUNCE_ENABLED_KEY = "haptics_bounce_enabled"
        const val HAPTICS_REGULAR_STRENGTH_KEY = "haptics_regular_strength"
        const val HAPTICS_HOLD_STRENGTH_KEY = "haptics_hold_strength"
        const val HAPTICS_RENDERER_STRENGTH_KEY = "haptics_renderer_strength"
        const val HAPTICS_LANGUAGE_STRENGTH_KEY = "haptics_language_strength"
        const val HAPTICS_BOUNCE_STRENGTH_KEY = "haptics_bounce_strength"
        const val HAPTICS_BOUNCE_RETURN_STRENGTH_KEY = "haptics_bounce_return_strength"

        // Particle refresh rate
        const val PARTICLE_NATIVE_REFRESH_RATE_KEY = "particle_native_refresh_rate"
        const val PARTICLE_QUARTER_REFRESH_RATE_KEY = "particle_quarter_refresh_rate"

        // Matrix preferences
        const val MATRIX_MODE_KEY = "matrix_mode"
        const val MATRIX_SPEED_KEY = "matrix_speed"
        const val MATRIX_DENSITY_KEY = "matrix_density"
        const val MATRIX_FONT_SIZE_KEY = "matrix_font_size"
        const val MATRIX_FADE_LENGTH_KEY = "matrix_fade_length"
        const val MATRIX_BG_ALPHA_KEY = "matrix_bg_alpha"

        // OLED preferences
        const val OLED_MODE_KEY = "oled_mode"
        const val OLED_ACCENT_COLOR_KEY = "oled_accent_color"
        const val USE_DYNAMIC_COLOR_OLED_KEY = "use_dynamic_color_oled"

        // Notification preferences
        // Keep these names aligned with the live GAMA UI/backup schema.
        const val NOTIFICATIONS_ENABLED_KEY = "notif_enabled"
        const val NOTIF_INTERVAL_INDEX_KEY = "notif_interval_idx"
        const val LAST_NOTIF_SENT_TIME_KEY = "notif_last_sent"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pendingSaveJob: Job? = null

    /**
     * Checks if preferences need to be migrated to the current version.
     * Returns true if migration occurred.
     */
    fun migratePreferencesIfNeeded(): Boolean {
        val savedPrefsVersion = prefs.getInt(PREFS_VERSION_KEY, 0)

        return if (savedPrefsVersion < PREFS_VERSION) {
            // Migrations must be additive. Clearing the preference file would
            // silently reset every user setting on a schema bump.
            prefs.edit().apply {
                putInt(PREFS_VERSION_KEY, PREFS_VERSION)
                if (!prefs.contains(ANIMATION_SPEED_KEY)) {
                    putInt(ANIMATION_SPEED_KEY, 1) // Default for genuinely new keys
                }
            }.apply()

            true
        } else {
            false
        }
    }

    /**
     * Saves all preferences asynchronously.
     * Captures current state values and persists them to SharedPreferences.
     */
    fun savePreferences(
        excludedAppsSnapshot: Set<String>,
        animationLevel: Int,
        gradientEnabled: Boolean,
        particlesEnabled: Boolean,
        particleSpeed: Int,
        particleParallaxEnabled: Boolean,
        particleParallaxSensitivity: Int,
        particleStarMode: Boolean,
        particleTimeMode: Boolean,
        timeOffsetHours: Float,
        particleCount: Int,
        particleCountCustom: Int,
        blurEnabled: Boolean,
        themePreference: Int,
        useDynamicColor: Boolean,
        advancedColorPicker: Boolean,
        customAccentColor: Int,
        customGradientStart: Int,
        customGradientEnd: Int,
        uiScale: Int,
        verboseMode: Boolean,
        aggressiveMode: Boolean,
        killLauncher: Boolean,
        killKeyboard: Boolean,
        showGpuWatchButton: Boolean,
        staggerEnabled: Boolean,
        backButtonAvoidanceEnabled: Boolean,
        backButtonInversed: Boolean,
        shadowsEnabled: Boolean,
        hapticsEnabled: Boolean,
        hapticsRegularEnabled: Boolean,
        hapticsHoldEnabled: Boolean,
        hapticsRendererEnabled: Boolean,
        hapticsLanguageEnabled: Boolean,
        hapticsBounceEnabled: Boolean,
        hapticsRegularStrength: Int,
        hapticsHoldStrength: Int,
        hapticsRendererStrength: Int,
        hapticsLanguageStrength: Int,
        hapticsBounceStrength: Int,
        hapticsBounceReturnStrength: Int,
        particleNativeRefreshRate: Int,
        particleQuarterRefreshRate: Int,
        matrixMode: Int,
        matrixSpeed: Int,
        matrixDensity: Int,
        matrixFontSize: Int,
        matrixFadeLength: Int,
        matrixBgAlpha: Int,
        oledMode: Boolean,
        oledAccentColor: Int,
        useDynamicColorOLED: Boolean,
        dismissOnClickOutside: Boolean,
        notificationsEnabled: Boolean,
        notifIntervalIndex: Int,
        lastNotifSentTime: Long,
        notifPermissionRequested: Boolean
    ) {
        pendingSaveJob?.cancel()
        pendingSaveJob = scope.launch {
            prefs.edit().apply {
                // Animation preferences
                putInt(ANIMATION_LEVEL_KEY, animationLevel)
                putBoolean(GRADIENT_ENABLED_KEY, gradientEnabled)

                // Particle preferences
                putBoolean(PARTICLES_ENABLED_KEY, particlesEnabled)
                putInt(PARTICLE_SPEED_KEY, particleSpeed)
                putBoolean(PARTICLE_PARALLAX_ENABLED_KEY, particleParallaxEnabled)
                putInt(PARTICLE_PARALLAX_SENSITIVITY_KEY, particleParallaxSensitivity)
                putBoolean(PARTICLE_STAR_MODE_KEY, particleStarMode)
                putBoolean(PARTICLE_TIME_MODE_KEY, particleTimeMode)
                putFloat(TIME_OFFSET_HOURS_KEY, timeOffsetHours)
                putInt(PARTICLE_COUNT_KEY, particleCount)
                putInt(PARTICLE_COUNT_CUSTOM_KEY, particleCountCustom)

                // UI preferences
                putBoolean(BLUR_ENABLED_KEY, blurEnabled)
                putInt(THEME_PREFERENCE_KEY, themePreference)
                putBoolean(USE_DYNAMIC_COLOR_KEY, useDynamicColor)
                putBoolean(ADVANCED_COLOR_PICKER_KEY, advancedColorPicker)
                putInt(CUSTOM_ACCENT_KEY, customAccentColor)
                putInt(CUSTOM_GRADIENT_START_KEY, customGradientStart)
                putInt(CUSTOM_GRADIENT_END_KEY, customGradientEnd)
                putInt(UI_SCALE_KEY, uiScale)

                // System preferences
                putBoolean(VERBOSE_MODE_KEY, verboseMode)
                putBoolean(AGGRESSIVE_MODE_KEY, aggressiveMode)
                putBoolean(KILL_LAUNCHER_KEY, killLauncher)
                putBoolean(KILL_KEYBOARD_KEY, killKeyboard)
                putBoolean(SHOW_GPUWATCH_BUTTON_KEY, showGpuWatchButton)
                putBoolean(STAGGER_ENABLED_KEY, staggerEnabled)
                putBoolean(BACK_BUTTON_AVOIDANCE_ENABLED_KEY, backButtonAvoidanceEnabled)
                putBoolean(BACK_BUTTON_INVERSED_KEY, backButtonInversed)
                putBoolean(SHADOWS_ENABLED_KEY, shadowsEnabled)

                // Haptic preferences
                putBoolean(HAPTICS_ENABLED_KEY, hapticsEnabled)
                putBoolean(HAPTICS_REGULAR_ENABLED_KEY, hapticsRegularEnabled)
                putBoolean(HAPTICS_HOLD_ENABLED_KEY, hapticsHoldEnabled)
                putBoolean(HAPTICS_RENDERER_ENABLED_KEY, hapticsRendererEnabled)
                putBoolean(HAPTICS_LANGUAGE_ENABLED_KEY, hapticsLanguageEnabled)
                putBoolean(HAPTICS_BOUNCE_ENABLED_KEY, hapticsBounceEnabled)
                putInt(HAPTICS_REGULAR_STRENGTH_KEY, hapticsRegularStrength)
                putInt(HAPTICS_HOLD_STRENGTH_KEY, hapticsHoldStrength)
                putInt(HAPTICS_RENDERER_STRENGTH_KEY, hapticsRendererStrength)
                putInt(HAPTICS_LANGUAGE_STRENGTH_KEY, hapticsLanguageStrength)
                putInt(HAPTICS_BOUNCE_STRENGTH_KEY, hapticsBounceStrength)
                putInt(HAPTICS_BOUNCE_RETURN_STRENGTH_KEY, hapticsBounceReturnStrength)

                // Particle refresh rate
                putInt(PARTICLE_NATIVE_REFRESH_RATE_KEY, particleNativeRefreshRate)
                putInt(PARTICLE_QUARTER_REFRESH_RATE_KEY, particleQuarterRefreshRate)

                // Matrix preferences
                putInt(MATRIX_MODE_KEY, matrixMode)
                putInt(MATRIX_SPEED_KEY, matrixSpeed)
                putInt(MATRIX_DENSITY_KEY, matrixDensity)
                putInt(MATRIX_FONT_SIZE_KEY, matrixFontSize)
                putInt(MATRIX_FADE_LENGTH_KEY, matrixFadeLength)
                putInt(MATRIX_BG_ALPHA_KEY, matrixBgAlpha)

                // OLED preferences
                putBoolean(OLED_MODE_KEY, oledMode)
                putInt(OLED_ACCENT_COLOR_KEY, oledAccentColor)
                putBoolean(USE_DYNAMIC_COLOR_OLED_KEY, useDynamicColorOLED)

                // User preferences
                // `dismissOnClickOutside` is a dialog behavior preference; it
                // must not overwrite the one-time button-label onboarding flag.
                putBoolean("dismiss_on_click_outside", dismissOnClickOutside)
                // button_labels_shown is written by the launch onboarding flow.
                putBoolean(NOTIF_PERM_REQUESTED_KEY, notifPermissionRequested)
                putBoolean(NOTIFICATIONS_ENABLED_KEY, notificationsEnabled)
                putInt(NOTIF_INTERVAL_INDEX_KEY, notifIntervalIndex)
                putLong(LAST_NOTIF_SENT_TIME_KEY, lastNotifSentTime)
                putStringSet(EXCLUDED_APPS_KEY, excludedAppsSnapshot)
                putInt(PREFS_VERSION_KEY, PREFS_VERSION)
            }.apply()
        }
    }

    /**
     * Gets user name from preferences
     */
    fun getUserName(default: String = ""): String = prefs.getString(USER_NAME_KEY, default) ?: default

    /**
     * Gets excluded apps list from preferences
     */
    fun getExcludedApps(default: Set<String> = emptySet()): Set<String> =
        prefs.getStringSet(EXCLUDED_APPS_KEY, default) ?: default

    /**
     * Gets button labels shown preference
     */
    fun getButtonLabelsShown(default: Boolean = false): Boolean =
        prefs.getBoolean(BUTTON_LABELS_SHOWN_KEY, default)

    /**
     * Gets notification permission requested preference
     */
    fun getNotifPermissionRequested(default: Boolean = false): Boolean =
        prefs.getBoolean(NOTIF_PERM_REQUESTED_KEY, default)

    // Add getter methods for all preference keys...
    // Getters for animation preferences
    fun getAnimationLevel(default: Int = 2): Int = prefs.getInt(ANIMATION_LEVEL_KEY, default)
    fun getAnimationSpeed(default: Int = 1): Int = prefs.getInt(ANIMATION_SPEED_KEY, default)
    fun getGradientEnabled(default: Boolean = true): Boolean = prefs.getBoolean(GRADIENT_ENABLED_KEY, default)

    // Getters for particle preferences
    fun getParticlesEnabled(default: Boolean = true): Boolean = prefs.getBoolean(PARTICLES_ENABLED_KEY, default)
    fun getParticleSpeed(default: Int = 2): Int = prefs.getInt(PARTICLE_SPEED_KEY, default)
    fun getParticleParallaxEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(PARTICLE_PARALLAX_ENABLED_KEY, default)

    fun getParticleParallaxSensitivity(default: Int = 2): Int = prefs.getInt(PARTICLE_PARALLAX_SENSITIVITY_KEY, default)
    fun getParticleStarMode(default: Boolean = false): Boolean = prefs.getBoolean(PARTICLE_STAR_MODE_KEY, default)
    fun getParticleTimeMode(default: Boolean = false): Boolean = prefs.getBoolean(PARTICLE_TIME_MODE_KEY, default)
    fun getTimeOffsetHours(default: Float = 0f): Float = prefs.getFloat(TIME_OFFSET_HOURS_KEY, default)
    fun getParticleCount(default: Int = 2): Int = prefs.getInt(PARTICLE_COUNT_KEY, default)
    fun getParticleCountCustom(default: Int = 150): Int = prefs.getInt(PARTICLE_COUNT_CUSTOM_KEY, default)

    // Getters for UI preferences
    fun getBlurEnabled(default: Boolean = true): Boolean = prefs.getBoolean(BLUR_ENABLED_KEY, default)
    fun getThemePreference(default: Int = 2): Int = prefs.getInt(THEME_PREFERENCE_KEY, default)
    fun getUseDynamicColor(default: Boolean = true): Boolean = prefs.getBoolean(USE_DYNAMIC_COLOR_KEY, default)
    fun getAdvancedColorPicker(default: Boolean = false): Boolean = prefs.getBoolean(ADVANCED_COLOR_PICKER_KEY, default)
    fun getCustomAccentColor(default: Int = 0xFFFF0000.toInt()): Int = prefs.getInt(CUSTOM_ACCENT_KEY, default)
    fun getCustomGradientStart(default: Int = 0xFFFF0000.toInt()): Int =
        prefs.getInt(CUSTOM_GRADIENT_START_KEY, default)

    fun getCustomGradientEnd(default: Int = 0xFF0000FF.toInt()): Int = prefs.getInt(CUSTOM_GRADIENT_END_KEY, default)
    fun getUiScale(default: Int = 2): Int = prefs.getInt(UI_SCALE_KEY, default)

    // Getters for system preferences
    fun getVerboseMode(default: Boolean = false): Boolean = prefs.getBoolean(VERBOSE_MODE_KEY, default)
    fun getAggressiveMode(default: Boolean = false): Boolean = prefs.getBoolean(AGGRESSIVE_MODE_KEY, default)
    fun getKillLauncher(default: Boolean = false): Boolean = prefs.getBoolean(KILL_LAUNCHER_KEY, default)
    fun getKillKeyboard(default: Boolean = false): Boolean = prefs.getBoolean(KILL_KEYBOARD_KEY, default)
    fun getShowGpuWatchButton(default: Boolean = true): Boolean = prefs.getBoolean(SHOW_GPUWATCH_BUTTON_KEY, default)
    fun getStaggerEnabled(default: Boolean = false): Boolean = prefs.getBoolean(STAGGER_ENABLED_KEY, default)
    fun getBackButtonAvoidanceEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(BACK_BUTTON_AVOIDANCE_ENABLED_KEY, default)

    fun getBackButtonInversed(default: Boolean = false): Boolean = prefs.getBoolean(BACK_BUTTON_INVERSED_KEY, default)
    fun getShadowsEnabled(default: Boolean = true): Boolean = prefs.getBoolean(SHADOWS_ENABLED_KEY, default)

    // Getters for haptic preferences
    fun getHapticsEnabled(default: Boolean = true): Boolean = prefs.getBoolean(HAPTICS_ENABLED_KEY, default)
    fun getHapticsRegularEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(HAPTICS_REGULAR_ENABLED_KEY, default)

    fun getHapticsHoldEnabled(default: Boolean = true): Boolean = prefs.getBoolean(HAPTICS_HOLD_ENABLED_KEY, default)
    fun getHapticsRendererEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(HAPTICS_RENDERER_ENABLED_KEY, default)

    fun getHapticsLanguageEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(HAPTICS_LANGUAGE_ENABLED_KEY, default)

    fun getHapticsBounceEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(HAPTICS_BOUNCE_ENABLED_KEY, default)

    fun getHapticsRegularStrength(default: Int = 2): Int = prefs.getInt(HAPTICS_REGULAR_STRENGTH_KEY, default)
    fun getHapticsHoldStrength(default: Int = 2): Int = prefs.getInt(HAPTICS_HOLD_STRENGTH_KEY, default)
    fun getHapticsRendererStrength(default: Int = 2): Int = prefs.getInt(HAPTICS_RENDERER_STRENGTH_KEY, default)
    fun getHapticsLanguageStrength(default: Int = 2): Int = prefs.getInt(HAPTICS_LANGUAGE_STRENGTH_KEY, default)
    fun getHapticsBounceStrength(default: Int = 2): Int = prefs.getInt(HAPTICS_BOUNCE_STRENGTH_KEY, default)
    fun getHapticsBounceReturnStrength(default: Int = 2): Int =
        prefs.getInt(HAPTICS_BOUNCE_RETURN_STRENGTH_KEY, default)

    // Getters for particle refresh rate
    fun getParticleNativeRefreshRate(default: Int = 2): Int = prefs.getInt(PARTICLE_NATIVE_REFRESH_RATE_KEY, default)
    fun getParticleQuarterRefreshRate(default: Int = 2): Int = prefs.getInt(PARTICLE_QUARTER_REFRESH_RATE_KEY, default)

    // Getters for matrix preferences
    fun getMatrixMode(default: Int = 0): Int = prefs.getInt(MATRIX_MODE_KEY, default)
    fun getMatrixSpeed(default: Int = 2): Int = prefs.getInt(MATRIX_SPEED_KEY, default)
    fun getMatrixDensity(default: Int = 2): Int = prefs.getInt(MATRIX_DENSITY_KEY, default)
    fun getMatrixFontSize(default: Int = 2): Int = prefs.getInt(MATRIX_FONT_SIZE_KEY, default)
    fun getMatrixFadeLength(default: Int = 2): Int = prefs.getInt(MATRIX_FADE_LENGTH_KEY, default)
    fun getMatrixBgAlpha(default: Int = 128): Int = prefs.getInt(MATRIX_BG_ALPHA_KEY, default)

    // Getters for OLED preferences
    fun getOledMode(default: Boolean = false): Boolean = prefs.getBoolean(OLED_MODE_KEY, default)
    fun getOledAccentColor(default: Int = 0xFFFF0000.toInt()): Int = prefs.getInt(OLED_ACCENT_COLOR_KEY, default)
    fun getUseDynamicColorOLED(default: Boolean = true): Boolean = prefs.getBoolean(USE_DYNAMIC_COLOR_OLED_KEY, default)

    // Getters for notification preferences
    fun getNotificationsEnabled(default: Boolean = true): Boolean = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, default)
    fun getNotifIntervalIndex(default: Int = 2): Int = prefs.getInt(NOTIF_INTERVAL_INDEX_KEY, default)
    fun getLastNotifSentTime(default: Long = 0L): Long = prefs.getLong(LAST_NOTIF_SENT_TIME_KEY, default)

    // Utility extension functions for Compose


    /**
     * Composable extension function to create mutable state variables
     * initialized from preferences with proper default values.
     */
    fun <T> mutableStateFromPref(prefValue: T, default: T): MutableState<T> {
        return mutableStateOf(prefValue ?: default)
    }
}
