package app.gamenative.ui.screen.wow

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import app.gamenative.ui.util.SnackbarManager
import app.gamenative.PluviaApp
import app.gamenative.externaldisplay.IMEInputReceiver

/** The password is never displayed, logged, or submitted automatically. */
object SavedLoginTools {
    fun show(context: Context, beforeFill: (() -> Unit)? = null) {
        val login = BattleNetSignIn.load(context)
        if (login == null) {
            AlertDialog.Builder(context)
                .setTitle("Saved login")
                .setMessage("No readable saved password. Unlock this Thor and try again, or use Update Login on the launcher.")
                .setPositiveButton("OK", null).show()
            return
        }
        val receiver = PluviaApp.imeInputReceiver ?: PluviaApp.xServerView?.getxServer()?.let { IMEInputReceiver(context, it) }
        val canFill = beforeFill != null && receiver != null
        AlertDialog.Builder(context)
            .setTitle("Saved login")
            .setMessage(
                (if (canFill) "Select WoW's password box first. Fill replaces the focused field without pressing Log In. " else "Your password is saved encrypted on this Thor. ") +
                    "Copy hides the clipboard preview and clears it after 30 seconds while this app is open.",
            )
            .setPositiveButton(if (canFill) "Fill password field" else "Copy password temporarily") { _, _ ->
                if (canFill) {
                    beforeFill?.invoke()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (runCatching { receiver!!.fillSecretText(login.password) }.getOrDefault(false)) {
                            SnackbarManager.show("Password filled. Press Log In when ready.")
                        } else {
                            SnackbarManager.show("This password needs keyboard paste. Use Copy password instead.")
                        }
                    }, 250L)
                } else copy(context, login.password)
            }
            .apply {
                if (canFill) setNeutralButton("Copy password temporarily") { _, _ -> copy(context, login.password) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copy(context: Context, password: String) {
        if (runCatching { SavedPasswordClipboard.copy(context, password) }.isSuccess) {
            SnackbarManager.show("Password copied temporarily. Paste it into WoW's password field.")
        } else {
            SnackbarManager.show("Clipboard unavailable. Return to the launcher and try again.")
        }
    }
}
