package app.gamenative.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import app.gamenative.BuildConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

object AppUpdater {
    private const val RELEASES_URL = "https://api.github.com/repos/wyattabuntjer/AYN-Thor-WoW-Launcher/releases/latest"
    private const val RELEASES_LIST_URL = "https://api.github.com/repos/wyattabuntjer/AYN-Thor-WoW-Launcher/releases?per_page=10"
    private const val KEY_BETA = "beta_updates"

    data class Release(
        val version: String,
        val name: String,
        val notes: String,
        val downloadUrl: String,
        val sizeBytes: Long,
    )

    /** A version split into its numbers and an optional pre-release tag ("2.3.3b" -> [2,3,3] + "b"). */
    private data class Version(val numbers: List<Int>, val pre: String?)

    private fun parseVersion(raw: String): Version {
        val v = raw.trim().trimStart('v', 'V').substringBefore('+')
        val match = Regex("^(\\d+(?:\\.\\d+)*)(.*)$").find(v) ?: return Version(emptyList(), null)
        val numbers = match.groupValues[1].split('.').mapNotNull { it.toIntOrNull() }
        val pre = match.groupValues[2].trimStart('-', '_', '.').lowercase().ifBlank { null }
        return Version(numbers, pre)
    }

    /** True for tags like "v2.3.3b", "v2.3.3-beta1" or "v2.4.0-rc1". */
    fun isPrerelease(version: String): Boolean = parseVersion(version).pre != null

    private fun compareVersions(a: String, b: String): Int {
        val l = parseVersion(a)
        val c = parseVersion(b)
        for (i in 0 until maxOf(l.numbers.size, c.numbers.size)) {
            val lv = l.numbers.getOrElse(i) { 0 }
            val cv = c.numbers.getOrElse(i) { 0 }
            if (lv != cv) return lv.compareTo(cv)
        }
        // Same numbers: a final release is newer than any beta of it.
        return when {
            l.pre == null && c.pre == null -> 0
            l.pre == null -> 1
            c.pre == null -> -1
            else -> l.pre.compareTo(c.pre)
        }
    }

    fun isNewer(latest: String, current: String = BuildConfig.VERSION_NAME): Boolean =
        compareVersions(latest, current) > 0

    /** Whether the person opted in to beta (pre-release) app updates. Off by default. */
    fun betaEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_BETA, false)

    fun setBetaEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BETA, enabled).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)

    fun parseRelease(
        jsonString: String,
        currentVersion: String = BuildConfig.VERSION_NAME,
        includeBeta: Boolean = false,
    ): Release? = runCatching {
        parseReleaseObject(JSONObject(jsonString), currentVersion, includeBeta)
    }.getOrNull()

    private fun parseReleaseObject(json: JSONObject, currentVersion: String, includeBeta: Boolean): Release? {
        if (json.optBoolean("draft")) return null
        val tag = json.optString("tag_name")
        // Betas are only offered to people who opted in, even if the release wasn't flagged as a pre-release.
        if (!includeBeta && (json.optBoolean("prerelease") || isPrerelease(tag))) return null
        if (!isNewer(tag, currentVersion)) return null

        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                return Release(
                    version = tag.trimStart('v', 'V'),
                    name = json.optString("name", tag),
                    notes = json.optString("body"),
                    downloadUrl = url,
                    sizeBytes = asset.optLong("size", 0L),
                )
            }
        }
        return null
    }

    /** Newest usable release in a releases list (the /releases endpoint). */
    fun parseReleaseList(
        jsonString: String,
        currentVersion: String = BuildConfig.VERSION_NAME,
        includeBeta: Boolean = true,
    ): Release? = runCatching {
        val array = JSONArray(jsonString)
        (0 until array.length())
            .mapNotNull { array.optJSONObject(it) }
            .mapNotNull { parseReleaseObject(it, currentVersion, includeBeta) }
            .maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
    }.getOrNull()

    suspend fun check(
        currentVersion: String = BuildConfig.VERSION_NAME,
        includeBeta: Boolean = false,
    ): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(if (includeBeta) RELEASES_LIST_URL else RELEASES_URL)
                .header("User-Agent", "WoW-Forever-Android")
                .header("Accept", "application/vnd.github+json")
                .build()
            Net.http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                if (includeBeta) parseReleaseList(body, currentVersion, true) else parseRelease(body, currentVersion, false)
            }
        }.getOrElse {
            Timber.w(it, "AppUpdater check failed")
            null
        }
    }

    suspend fun download(
        context: Context,
        release: Release,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "WoW-Forever-${release.version}.apk")
        if (dest.exists() && dest.length() == release.sizeBytes && release.sizeBytes > 0) {
            onProgress(1f)
            return@withContext dest
        }
        Net.fetchFile(release.downloadUrl, dest, onProgress)
        dest
    }

    fun canInstall(context: Context): Boolean = runCatching {
        context.packageManager.canRequestPackageInstalls()
    }.getOrElse {
        Timber.w(it, "canRequestPackageInstalls check failed")
        false
    }

    fun openInstallPermissionSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        }
    }

    fun install(context: Context, apkFile: File) {
        if (!canInstall(context)) {
            openInstallPermissionSettings(context)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
