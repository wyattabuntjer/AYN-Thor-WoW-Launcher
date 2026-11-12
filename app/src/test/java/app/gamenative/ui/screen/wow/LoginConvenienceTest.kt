package app.gamenative.ui.screen.wow

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Looper
import com.winlator.container.Container
import java.time.Duration
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LoginConvenienceTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val clipboard get() = context.getSystemService(ClipboardManager::class.java)

    @Before fun reset() {
        context.getSharedPreferences("battle_net_login", 0).edit().clear().commit()
        context.getSharedPreferences("wow_session", 0).edit().clear().commit()
        clipboard.clearPrimaryClip()
    }

    private fun seedEncryptedFixture() {
        context.getSharedPreferences("battle_net_login", 0).edit()
            .putString("email", "fixture@example.invalid")
            .putString("password", "ZW5jcnlwdGVkLWZpeHR1cmU=").commit()
    }

    @Test fun transientDecryptionFailureKeepsEncryptedPassword() {
        seedEncryptedFixture()
        assertNull(BattleNetSignIn.load(context) { throw IllegalStateException("Keystore unavailable") })
        assertTrue(BattleNetSignIn.hasSavedLogin(context))
        assertEquals("ZW5jcnlwdGVkLWZpeHR1cmU=", context.getSharedPreferences("battle_net_login", 0).getString("password", null))
    }

    @Test fun disablingAutomaticLoginKeepsPasswordAvailable() {
        seedEncryptedFixture()
        BattleNetSignIn.setAutoLoginEnabled(context, false)
        assertFalse(BattleNetSignIn.autoLoginEnabled(context))
        assertTrue(BattleNetSignIn.hasSavedLogin(context))
        assertEquals("fixture-only", BattleNetSignIn.load(context) { "fixture-only".toByteArray() }?.password)
    }

    @Test fun credentialDebugStringDoesNotExposeEitherField() {
        val login = BattleNetSignIn.Login("fixture@example.invalid", "fixture-only")
        assertFalse(login.toString().contains(login.email))
        assertFalse(login.toString().contains(login.password))
    }

    @Test fun copiedPasswordIsSensitiveAndExpires() {
        SavedPasswordClipboard.copy(context, "fixture-only")
        assertTrue(clipboard.primaryClipDescription!!.extras!!.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
        assertFalse(clipboard.hasPrimaryClip())
    }

    @Test fun expiryPreservesSomethingCopiedAfterThePassword() {
        SavedPasswordClipboard.copy(context, "fixture-only")
        clipboard.setPrimaryClip(ClipData.newPlainText("Other", "keep-this"))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
        assertEquals("keep-this", clipboard.primaryClip!!.getItemAt(0).text.toString())
    }

    @Test fun forgettingCredentialsAlsoClearsTheirOwnClipboardCopy() {
        seedEncryptedFixture()
        SavedPasswordClipboard.copy(context, "fixture-only")
        BattleNetSignIn.forget(context)
        assertFalse(BattleNetSignIn.hasSavedLogin(context))
        assertFalse(clipboard.hasPrimaryClip())
    }

    @Test fun keepRunningUsesNeverSuspendAndRestoresPreviousPolicyWhenDisabled() {
        val container = Container("fixture")
        container.suspendPolicy = Container.SUSPEND_POLICY_AUTO
        WoWSessionSettings.applyTo(context, container)
        assertEquals(Container.SUSPEND_POLICY_NEVER, container.suspendPolicy)
        WoWSessionSettings.setKeepRunning(context, false)
        WoWSessionSettings.applyTo(context, container)
        assertEquals(Container.SUSPEND_POLICY_AUTO, container.suspendPolicy)
    }
}
