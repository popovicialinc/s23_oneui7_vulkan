@file:Suppress("RestrictedApi")

package com.popovicialinc.gama

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GamaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = context.getSharedPreferences("gama_prefs", Context.MODE_PRIVATE)
            val renderer = RendererState.getRenderer(prefs)
            Column(
                modifier = GlanceModifier.fillMaxSize().background(R.color.widget_background).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GAMA", style = TextStyle(color = ColorProvider(R.color.white), fontSize = 14.sp))
                Spacer(GlanceModifier.height(6.dp))
                Text(renderer, style = TextStyle(color = ColorProvider(R.color.widget_text_secondary), fontSize = 12.sp))
                Spacer(GlanceModifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Vulkan",
                        modifier = GlanceModifier.background(R.color.widget_vulkan).padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable(actionRunCallback<VulkanRendererAction>()),
                        style = TextStyle(color = ColorProvider(R.color.white), fontSize = 11.sp)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        "OpenGL",
                        modifier = GlanceModifier.background(R.color.widget_opengl).padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable(actionRunCallback<OpenGlRendererAction>()),
                        style = TextStyle(color = ColorProvider(R.color.white), fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

class GamaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GamaWidget()
}

private suspend fun applyRenderer(context: Context, glanceId: GlanceId, renderer: String) {
        withContext(Dispatchers.IO) {
            if (!ShizukuHelper.isBackendReady() && !ShizukuHelper.refreshRootAvailability()) return@withContext
            val prefs = context.getSharedPreferences("gama_prefs", Context.MODE_PRIVATE)
            val aggressiveMode = prefs.getBoolean("aggressive_mode", false)
            val killLauncher = prefs.getBoolean("kill_launcher", false)
            val killKeyboard = prefs.getBoolean("kill_keyboard", false)
            val excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet()
            val applied = if (renderer == RendererState.RENDERER_VULKAN) {
                ShizukuHelper.runVulkanSuspend(
                    context = context,
                    aggressiveMode = aggressiveMode,
                    killLauncher = killLauncher,
                    killKeyboard = killKeyboard,
                    excludedApps = excludedApps,
                    targetedApps = emptySet(),
                    onStatusUpdate = {}
                )
            } else {
                ShizukuHelper.runOpenGLSuspend(
                    context = context,
                    aggressiveMode = aggressiveMode,
                    killLauncher = killLauncher,
                    killKeyboard = killKeyboard,
                    excludedApps = excludedApps,
                    targetedApps = emptySet(),
                    onStatusUpdate = {}
                )
            }
            if (applied) RendererState.recordSwitch(prefs, renderer)
            GamaWidget().update(context, glanceId)
        }
}

class VulkanRendererAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        applyRenderer(context, glanceId, RendererState.RENDERER_VULKAN)
    }
}

class OpenGlRendererAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        applyRenderer(context, glanceId, RendererState.RENDERER_OPENGL)
    }
}
