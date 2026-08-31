package com.popovicialinc.gama

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Executes Tasker renderer changes outside BroadcastReceiver's ANR deadline. */
class TaskerRendererWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "gama_tasker_renderer"
        const val INPUT_RENDERER = "renderer"
        const val INPUT_AGGRESSIVE = "aggressive"
    }

    override suspend fun doWork(): Result {
        val target = inputData.getString(INPUT_RENDERER) ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("gama_prefs", Context.MODE_PRIVATE)
        // An explicit Tasker aggressive:true request remains supported, while the
        // normal renderer settings apply consistently to automated switches too.
        val aggressive = inputData.getBoolean(INPUT_AGGRESSIVE, false) ||
            prefs.getBoolean("aggressive_mode", false)
        val killLauncher = prefs.getBoolean("kill_launcher", false)
        val killKeyboard = prefs.getBoolean("kill_keyboard", false)
        val excludedApps = prefs.getStringSet("excluded_apps", emptySet()) ?: emptySet()

        // Tasker can cold-start GAMA, so root has not necessarily been probed in
        // this process. A configured Tasker action is an explicit user request.
        val rootReady = ShizukuHelper.refreshRootAvailability()
        val shizukuReady = ShizukuHelper.checkBinder() && ShizukuHelper.checkPermission()
        if (!rootReady && !shizukuReady) return Result.failure()

        val applied = when (target) {
            RendererState.RENDERER_VULKAN -> ShizukuHelper.runVulkanSuspend(
                context = applicationContext,
                aggressiveMode = aggressive,
                killLauncher = killLauncher,
                killKeyboard = killKeyboard,
                excludedApps = excludedApps,
                targetedApps = emptySet(),
                onStatusUpdate = {}
            )
            RendererState.RENDERER_OPENGL -> ShizukuHelper.runOpenGLSuspend(
                context = applicationContext,
                aggressiveMode = aggressive,
                killLauncher = killLauncher,
                killKeyboard = killKeyboard,
                excludedApps = excludedApps,
                targetedApps = emptySet(),
                onStatusUpdate = {}
            )
            else -> return Result.failure()
        }
        if (!applied) return Result.failure()

        RendererState.recordSwitch(
            prefs,
            target
        )
        ShizukuHelper.refreshRendererViewSync(applicationContext)
        return Result.success()
    }
}
