package com.popovicialinc.gama.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Composable function to create and remember a PreferencesManager instance.
 */
@Composable
fun rememberPreferencesManager(context: Context): PreferencesManager {
    return remember { PreferencesManager(context) }
}
