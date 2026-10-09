package app.gamenative.ui.screen.wow

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import app.gamenative.ui.theme.WowCream
import app.gamenative.ui.theme.WowFrame
import app.gamenative.ui.theme.WowGold
import app.gamenative.ui.theme.WowStoneTop
import android.content.Context
import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.gamenative.Crypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

object BattleNetSignIn {
    data class Login(val email: String, val password: String)

    fun load(context: Context): Login? {
        val prefs = prefs(context)
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val sealed = prefs.getString(KEY_PASSWORD, null) ?: return null
        return try {
            Login(email, String(Crypto.decrypt(Base64.decode(sealed, Base64.NO_WRAP))))
        } catch (e: Exception) {
            Timber.w(e, "Saved Battle.net login unreadable, clearing it")
            forget(context)
            null
        }
    }

    fun save(context: Context, login: Login) {
        val sealed = Base64.encodeToString(Crypto.encrypt(login.password.toByteArray()), Base64.NO_WRAP)
        prefs(context).edit().putString(KEY_EMAIL, login.email).putString(KEY_PASSWORD, sealed).apply()
    }

    /** Clears the saved email and password. The auto-login choice is kept. */
    fun forget(context: Context) {
        prefs(context).edit().remove(KEY_EMAIL).remove(KEY_PASSWORD).apply()
    }

    /** Whether the launcher signs in for you. When off, no login.txt is ever written and nothing is stored. */
    fun autoLoginEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_LOGIN, true)

    fun setAutoLoginEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }

    /**
     * login.txt holds the password in plain text, so it should only exist while the client starts.
     * Removes it after [delayMs] on an app-wide scope, so leaving the launcher screen doesn't cancel it.
     */
    fun scheduleLoginFileRemoval(gameRoot: File, delayMs: Long = LOGIN_FILE_LIFETIME_MS) {
        cleanupScope.launch {
            delay(delayMs)
            removeLoginFile(gameRoot)
        }
    }

    fun writeLoginFile(gameRoot: File, login: Login) {
        try {
            val file = File(File(gameRoot, WowClientDownloader.FLAVOR_DIR), "login.txt")
            file.parentFile?.mkdirs()
            file.writeText("${login.email}\n${login.password}\n")
        } catch (e: Exception) {
            Timber.e(e, "Failed to write login.txt")
        }
    }

    fun removeLoginFile(gameRoot: File) {
        try {
            val file = File(File(gameRoot, WowClientDownloader.FLAVOR_DIR), "login.txt")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove login.txt")
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "battle_net_login"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private const val KEY_AUTO_LOGIN = "auto_login"
    const val LOGIN_FILE_LIFETIME_MS = 60_000L
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Composable
fun BattleNetCredentialDialog(
    onDismiss: () -> Unit,
    onConfirm: (BattleNetSignIn.Login) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val emailFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(DIALOG_SETTLE_MS)
        emailFocus.requestFocus()
        keyboard?.show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WowStoneTop,
        titleContentColor = WowGold,
        textContentColor = WowCream,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(3.dp, WowFrame, RoundedCornerShape(10.dp)),
        title = { Text("Battle.net login") },
        text = {
            Column {
                Text("Saved encrypted on this device. WoW will automatically log in on launch.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.focusRequester(emailFocus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = email.isNotBlank() && password.isNotEmpty(),
                onClick = { onConfirm(BattleNetSignIn.Login(email.trim(), password)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val DIALOG_SETTLE_MS = 300L
