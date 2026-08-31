package com.popovicialinc.gama

import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// ── Tile localization helper ──────────────────────────────────────────────────
private fun android.content.Context.tileStr(section: String, key: String, fallback: String): String {
    return try {
        val prefs = getSharedPreferences("gama_prefs", android.content.Context.MODE_PRIVATE)
        val code = prefs.getString("selected_language", "en") ?: "en"
        if (code == "en") return fallback
        val raw = assets.open("translations/$code.json").bufferedReader().readText()
        org.json.JSONObject(raw).optJSONObject(section)?.optString(key)?.takeIf { it.isNotEmpty() } ?: fallback
    } catch (_: Exception) { fallback }
}

/**
 * Single Quick Settings tile that switches between the two renderers —
 * replaces the old separate Vulkan and OpenGL tiles.
 *
 * Tap → Vulkan if currently OpenGL, OpenGL if currently Vulkan.
 * The subtitle always shows the current renderer plus a hint of the next one.
 */
@RequiresApi(Build.VERSION_CODES.N)
class RendererToggleTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("gama_prefs", Context.MODE_PRIVATE)
        RendererState.reconcileRebootReset(prefs)
        val current = RendererState.getRenderer(prefs)
        val targetVulkan = current != RendererState.RENDERER_VULKAN
        val targetName = if (targetVulkan) RendererState.RENDERER_VULKAN else RendererState.RENDERER_OPENGL

        scope.launch {
            if (!ShizukuHelper.isBackendReady() && !ShizukuHelper.refreshRootAvailability()) {
                setTile(Tile.STATE_INACTIVE, applicationContext.tileStr("tile", "state_shizuku_not_running", "Shizuku isn't running"))
                return@launch
            }

            setTile(Tile.STATE_ACTIVE, applicationContext.tileStr("tile", "state_switching", "Switching…"))

            try {
                val applied = if (targetVulkan) {
                    ShizukuHelper.runVulkanSuspend(
                        context = applicationContext,
                        aggressiveMode = prefs.getBoolean("aggressive_mode", false),
                        killLauncher = prefs.getBoolean("kill_launcher", false),
                        killKeyboard = prefs.getBoolean("kill_keyboard", false),
                        excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet(),
                        targetedApps = emptySet(),
                        onStatusUpdate = {}
                    )
                } else {
                    ShizukuHelper.runOpenGLSuspend(
                        context = applicationContext,
                        aggressiveMode = prefs.getBoolean("aggressive_mode", false),
                        killLauncher = prefs.getBoolean("kill_launcher", false),
                        killKeyboard = prefs.getBoolean("kill_keyboard", false),
                        excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet(),
                        targetedApps = emptySet(),
                        onStatusUpdate = {}
                    )
                }
                if (applied) {
                    // Persist renderer + both switch timestamps so BootReceiver
                    // re-applies this choice after reboot. commit() because the
                    // SystemUI soft-crash can kill this process mid-switch.
                    RendererState.recordSwitch(prefs, targetName)
                    setTile(Tile.STATE_ACTIVE, null)
                } else {
                    setTile(
                        Tile.STATE_INACTIVE,
                        applicationContext.tileStr("tile", "state_failed", "Failed — tap to retry")
                    )
                }
            } catch (_: Exception) {
                setTile(Tile.STATE_INACTIVE, applicationContext.tileStr("tile", "state_failed", "Failed — tap to retry"))
            }
        }
    }

    private fun setTile(state: Int, subtitle: String?) {
        val tile = qsTile ?: return
        tile.label = applicationContext.tileStr("tile", "label", "GAMA · Renderer")
        tile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle ?: currentRendererSubtitle()
        }
        tile.updateTile()
    }

    private fun currentRendererSubtitle(): String {
        val prefs = getSharedPreferences("gama_prefs", Context.MODE_PRIVATE)
        RendererState.reconcileRebootReset(prefs)
        val renderer = RendererState.getRenderer(prefs)
        return if (renderer == RendererState.RENDERER_VULKAN) {
            applicationContext.tileStr("tile", "state_vulkan", "Vulkan · tap for OpenGL")
        } else {
            applicationContext.tileStr("tile", "state_opengl", "OpenGL · tap for Vulkan")
        }
    }

    private fun refreshTile() {
        setTile(Tile.STATE_ACTIVE, null)
    }
}
