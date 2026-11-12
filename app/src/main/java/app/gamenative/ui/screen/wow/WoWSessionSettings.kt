package app.gamenative.ui.screen.wow

import android.content.Context
import com.winlator.container.Container

object WoWSessionSettings {
    private fun prefs(context: Context) = context.getSharedPreferences("wow_session", Context.MODE_PRIVATE)

    fun keepRunning(context: Context): Boolean = prefs(context).getBoolean("keep_running", true)

    fun setKeepRunning(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("keep_running", enabled).apply()
    }

    fun applyTo(context: Context, container: Container) {
        if (keepRunning(context)) {
            if (container.suspendPolicy != Container.SUSPEND_POLICY_NEVER) {
                container.putExtra("wowPreviousSuspendPolicy", container.suspendPolicy)
            }
            container.suspendPolicy = Container.SUSPEND_POLICY_NEVER
        } else {
            container.suspendPolicy = container.getExtra("wowPreviousSuspendPolicy", Container.SUSPEND_POLICY_MANUAL)
        }
    }
}
