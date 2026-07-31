package com.popovicialinc.gama

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * Downloads and installs the official Shizuku APK so users never have to leave
 * GAMA. The APK comes straight from the RikkaApps GitHub release assets —
 * verified to actually be moe.shizuku.privileged.api before the installer is
 * launched.
 */
object ShizukuInstaller {

    // Official repo — the only source GAMA will ever download Shizuku from.
    private const val RELEASE_API_URL = "https://api.github.com/repos/RikkaApps/Shizuku/releases/latest"
    private const val RELEASE_PAGE_URL = "https://github.com/RikkaApps/Shizuku/releases/latest"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val MIN_APK_SIZE = 1_000_000L // ~2.5 MB is normal; anything under 1 MB is not Shizuku
    private const val USER_AGENT = "GAMA/1.4 (renderer manager; github.com/popovicialinc/gama)"

    // APK asset href inside the GitHub "expanded assets" page, e.g.
    // href="/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk"
    private val APK_HREF_REGEX = Regex("""href="([^"]+\.apk)"""")

    // Latest failure reason, filled in by the resolvers/downloader so the UI can
    // show exactly what went wrong instead of a generic message.
    private var lastError: String = ""

    // Session-install broadcast id for the confirmation callback.
    private const val ACTION_INSTALL_STATUS = "com.popovicialinc.gama.INSTALL_STATUS"
    private const val EXTRA_SESSION_ID = "session_id"

    /** Result of a download attempt — either [apkFile] (verified) or [error]. */
    data class DownloadResult(
        val apkFile: File?,
        val error: String
    )

    /**
     * Downloads the latest Shizuku release APK into the cache dir.
     *
     * @param onProgress called on the IO dispatcher with 0f..1f while streaming.
     * @return a [DownloadResult]; on failure [DownloadResult.error] carries a
     *         human-readable reason and any partially written file is deleted.
     */
    suspend fun downloadLatestApk(context: Context, onProgress: (Float) -> Unit): DownloadResult =
        withContext(Dispatchers.IO) {
            val target = File(context.cacheDir, "apk/shizuku_latest.apk")
            try {
                // ── 1. Resolve the newest release's APK URL ────────────────────
                // The JSON API is cleanest but only allows 60 anonymous requests
                // per hour PER IP — and mobile carriers share IPs, so it gets
                // rate-limited (HTTP 403) a lot. The plain release page has no
                // such limit, so it's the fallback whenever the API fails.
                val apkUrl = resolveApkUrlApi() ?: resolveApkUrlHtml()
                if (apkUrl == null) {
                    return@withContext DownloadResult(null, lastError)
                }

                // ── 2. Stream the APK to cache ─────────────────────────────────
                target.parentFile?.mkdirs()
                val dlConnection = URL(apkUrl).openConnection() as HttpURLConnection
                try {
                    dlConnection.setRequestProperty("User-Agent", USER_AGENT)
                    dlConnection.connectTimeout = 15_000
                    dlConnection.readTimeout = 15_000
                    dlConnection.instanceFollowRedirects = true
                    if (dlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                        lastError = "GitHub replied HTTP ${dlConnection.responseCode} while downloading the APK"
                        return@withContext DownloadResult(null, lastError)
                    }
                    val totalBytes = dlConnection.contentLengthLong
                    dlConnection.inputStream.use { input ->
                        FileOutputStream(target).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                } finally {
                    dlConnection.disconnect()
                }

                // ── 3. Verify: size, ZIP magic, and real package name ──────────
                if (target.length() < MIN_APK_SIZE) {
                    target.delete()
                    return@withContext DownloadResult(null, "Downloaded file is too small to be an APK")
                }
                target.inputStream().use { input ->
                    val magic = ByteArray(4)
                    if (input.read(magic) != 4 || magic[0] != 'P'.code.toByte() ||
                        magic[1] != 'K'.code.toByte() || magic[2] != 3.toByte() || magic[3] != 4.toByte()
                    ) {
                        target.delete()
                        return@withContext DownloadResult(null, "Downloaded file isn't a valid APK")
                    }
                }
                val pkgInfo = context.packageManager.getPackageArchiveInfo(target.absolutePath, 0)
                if (pkgInfo == null || pkgInfo.packageName != SHIZUKU_PACKAGE) {
                    target.delete()
                    return@withContext DownloadResult(null, "Downloaded file isn't Shizuku (${pkgInfo?.packageName ?: "unknown package"})")
                }

                DownloadResult(target, "")
            } catch (e: Exception) {
                target.delete()
                DownloadResult(null, "Error: ${e.message ?: e.javaClass.simpleName}")
            }
        }

    /**
     * Tries the GitHub JSON API first.
     * @return a direct APK download URL, or null (with [lastError] set).
     */
    private fun resolveApkUrlApi(): String? {
        try {
            val conn = URL(RELEASE_API_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                when (conn.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val json = conn.inputStream.bufferedReader().use { it.readText() }
                        val assets = JSONObject(json).getJSONArray("assets")
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                                val url = asset.optString("browser_download_url")
                                if (url.isNotBlank()) return url
                            }
                        }
                        lastError = "No APK found on the GitHub release page"
                    }
                    else -> lastError = "GitHub API rate limit or error (HTTP ${conn.responseCode})"
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            lastError = "GitHub API unreachable"
        }
        return null
    }

    /**
     * Rate-limit-free fallback: follows /releases/latest to the tag page, then
     * reads the "expanded assets" page which lists the APK download link.
     * @return a direct APK download URL, or null (with [lastError] set).
     */
    private fun resolveApkUrlHtml(): String? {
        try {
            val latest = URL(RELEASE_PAGE_URL).openConnection() as HttpURLConnection
            val tagUrl: String
            try {
                latest.setRequestProperty("User-Agent", USER_AGENT)
                latest.connectTimeout = 10_000
                latest.readTimeout = 10_000
                latest.instanceFollowRedirects = true
                val code = latest.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    lastError = "GitHub replied HTTP $code on the release page"
                    return null
                }
                // After following redirects this is https://github.com/.../releases/tag/<tag>
                tagUrl = latest.url.toString()
            } finally {
                latest.disconnect()
            }

            val assetsUrl = tagUrl.replace("/tag/", "/expanded_assets/")
            val page = URL(assetsUrl).openConnection() as HttpURLConnection
            try {
                page.setRequestProperty("User-Agent", USER_AGENT)
                page.connectTimeout = 10_000
                page.readTimeout = 10_000
                if (page.responseCode != HttpURLConnection.HTTP_OK) {
                    lastError = "GitHub replied HTTP ${page.responseCode} on the assets page"
                    return null
                }
                val html = page.inputStream.bufferedReader().use { it.readText() }
                val match = APK_HREF_REGEX.find(html) ?: run {
                    lastError = "No APK link found on the GitHub release page"
                    return null
                }
                val href = match.groupValues[1]
                return if (href.startsWith("http")) href else "https://github.com$href"
            } finally {
                page.disconnect()
            }
        } catch (e: Exception) {
            lastError = "Couldn't reach GitHub (${e.message ?: e.javaClass.simpleName})"
        }
        return null
    }

    /**
     * Installs the downloaded APK.
     *
     * Priority:
     *  1. Root  → silent `pm install -r`, no dialog at all.
     *  2. Default → a PackageInstaller session with requireUserAction=REQUIRED.
     *     This is the same mechanism F-Droid, Aurora Store and Morphe use: the
     *     system shows its normal "Install this app?" confirmation dialog and
     *     NOTHING else — no "Install unknown apps" settings detour.
     *
     * @return [InstallResult.Installed] once the system confirms the install,
     *         [InstallResult.Cancelled] if the user dismissed the dialog, or
     *         [InstallResult.Failed] with a human-readable reason.
     */
    suspend fun installApk(context: Context, apkFile: File): InstallResult {
        if (ShizukuHelper.isRootAvailable() && ShizukuHelper.installApkViaRoot(apkFile.absolutePath)) {
            return InstallResult.Installed
        }
        return installViaSession(context.applicationContext, apkFile)
    }

    private suspend fun installViaSession(context: Context, apkFile: File): InstallResult =
        withContext(Dispatchers.IO) {
            val installer = context.packageManager.packageInstaller

            val sessionId = try {
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
                ).apply {
                    setOriginatingUid(Process.myUid())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setPackageSource(PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Tells the system this install needs the user's confirmation —
                        // which means no "allow unknown apps" grant is required.
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                    }
                }
                installer.createSession(params)
            } catch (_: SecurityException) {
                return@withContext InstallResult.Failed(
                    "GAMA isn't allowed to open the installer on this device. " +
                        "Enable 'Install unknown apps' for GAMA in Settings, then retry."
                )
            } catch (e: Exception) {
                return@withContext InstallResult.Failed(
                    "Couldn't start the installer: ${e.message ?: e.javaClass.simpleName}"
                )
            }

            suspendCancellableCoroutine<InstallResult> { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (intent.getIntExtra(EXTRA_SESSION_ID, -1) != sessionId) return
                        when (val status = intent.getIntExtra(
                            PackageInstaller.EXTRA_STATUS,
                            PackageInstaller.STATUS_FAILURE
                        )) {
                            PackageInstaller.STATUS_SUCCESS -> {
                                runCatching { context.unregisterReceiver(this) }
                                cont.resume(InstallResult.Installed)
                            }

                            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                                // Hand the system's "Install?" confirmation to the user.
                                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                                }
                                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let { context.startActivity(it) }
                            }

                            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                                runCatching { context.unregisterReceiver(this) }
                                cont.resume(InstallResult.Cancelled)
                            }

                            else -> {
                                runCatching { context.unregisterReceiver(this) }
                                cont.resume(
                                    InstallResult.Failed(
                                        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                                            ?: "Installation failed (status $status)"
                                    )
                                )
                            }
                        }
                    }
                }

                val broadcastIntent = Intent(ACTION_INSTALL_STATUS).apply {
                    `package` = context.packageName
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
                val pending = PendingIntent.getBroadcast(
                    context, sessionId, broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(
                        receiver, IntentFilter(ACTION_INSTALL_STATUS),
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    context.registerReceiver(receiver, IntentFilter(ACTION_INSTALL_STATUS))
                }

                cont.invokeOnCancellation {
                    runCatching { context.unregisterReceiver(receiver) }
                    runCatching { installer.abandonSession(sessionId) }
                }

                try {
                    installer.openSession(sessionId).use { session ->
                        session.openWrite("base.apk", 0, apkFile.length()).use { out ->
                            apkFile.inputStream().use { it.copyTo(out) }
                            session.fsync(out)
                        }
                        session.commit(pending.intentSender)
                    }
                } catch (e: Exception) {
                    runCatching { context.unregisterReceiver(receiver) }
                    runCatching { installer.abandonSession(sessionId) }
                    if (cont.isActive) {
                        cont.resume(
                            InstallResult.Failed(
                                "Couldn't start the install: ${e.message ?: e.javaClass.simpleName}"
                            )
                        )
                    }
                }
            }
        }
}

/** Outcome of an APK install attempt. */
sealed class InstallResult {
    /** The package is installed (confirmed by the system, or silent via root). */
    data object Installed : InstallResult()

    /** The user dismissed the confirmation dialog. */
    data object Cancelled : InstallResult()

    /** Something went wrong; [reason] is human-readable. */
    data class Failed(val reason: String) : InstallResult()
}
