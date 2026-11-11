package app.gamenative.ui.screen.wow

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import java.util.UUID

/** Explicit copying only; marks the content sensitive and never clears someone else's later copy. */
internal object SavedPasswordClipboard {
    internal const val OWNER_KEY = "app.aynthorwow.password_clip"
    private var owner: String? = null
    private var expiresAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun copy(context: Context, password: String) {
        val token = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText("Saved password", password)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            putString(OWNER_KEY, token)
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        owner = token
        expiresAt = SystemClock.elapsedRealtime() + 30_000L
        handler.postDelayed({ clearExpired(context.applicationContext) }, 30_000L)
    }

    fun clearExpired(context: Context) {
        if (SystemClock.elapsedRealtime() < expiresAt) return
        clearOwned(context)
    }

    fun clearOwned(context: Context) {
        val token = owner ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        // Android can deny clipboard inspection in the background; onResume retries this.
        val description = runCatching { clipboard.primaryClipDescription }.getOrNull() ?: return
        if (description.extras?.getString(OWNER_KEY) == token) {
            if (runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }.isFailure) return
        }
        owner = null
    }
}
