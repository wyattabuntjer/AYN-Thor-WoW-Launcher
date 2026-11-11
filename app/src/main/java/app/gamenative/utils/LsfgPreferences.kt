package app.gamenative.utils

import android.content.Context
import com.winlator.container.Container

/** Multipliers are saved per WoW product, including changes in the in-game menu. */
object LsfgPreferences {
    private const val PREFS = "wow_lsfg"
    private const val PROFILE = "lsfgProfile"

    fun multiplier(context: Context, profile: String): Int =
        sanitize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(profile, 0))

    fun save(context: Context, profile: String, multiplier: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(profile, sanitize(multiplier)).apply()
    }

    fun applyTo(context: Context, container: Container, profile: String) {
        container.putExtra(PROFILE, profile)
        // Keep the LSFG menu available with a valid DLL even when the multiplier is Off.
        container.putExtra(LsfgVkManager.EXTRA_ARMED, LsfgDllStore.available(context).toString())
        container.putExtra(LsfgVkManager.EXTRA_MULTIPLIER, multiplier(context, profile).toString())
    }

    fun saveRuntime(context: Context, container: Container, multiplier: Int) {
        container.getExtra(PROFILE, "").takeIf { it.isNotBlank() }?.let { save(context, it, multiplier) }
    }

    private fun sanitize(value: Int): Int = if (value < 2) 0 else value.coerceIn(2, 4)
}
