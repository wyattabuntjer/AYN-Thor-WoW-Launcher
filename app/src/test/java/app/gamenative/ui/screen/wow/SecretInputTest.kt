package app.gamenative.ui.screen.wow

import android.app.Application
import android.view.KeyEvent
import app.gamenative.externaldisplay.IMEInputReceiver
import com.winlator.xserver.Keyboard
import com.winlator.xserver.XKeycode
import com.winlator.xserver.XServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SecretInputTest {
    private fun target(): Triple<IMEInputReceiver, XServer, Keyboard> {
        val server = mockk<XServer>(relaxed = true)
        val keyboard = mockk<Keyboard>(relaxed = true)
        XServer::class.java.getField("keyboard").apply { isAccessible = true }.set(server, keyboard)
        val context: Application = RuntimeEnvironment.getApplication()
        return Triple(IMEInputReceiver(context, server), server, keyboard)
    }

    @Test fun fillsMixedCaseAndSymbolsWithoutSubmitting() {
        val (receiver, server, keyboard) = target()
        val events = mutableListOf<KeyEvent>()
        every { keyboard.onKeyEvent(any()) } answers { events.add(firstArg()); true }
        assertTrue(receiver.fillSecretText("Aa@!9"))
        verify { server.injectKeyPress(XKeycode.KEY_CTRL_L) }
        verify { server.injectKeyRelease(XKeycode.KEY_CTRL_L) }
        verify { server.injectKeyPress(XKeycode.KEY_BKSP) }
        val down = events.filter { it.action == KeyEvent.ACTION_DOWN }
        assertEquals(listOf(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_9), down.map { it.keyCode })
        assertTrue(down[0].isShiftPressed)
        assertFalse(down[1].isShiftPressed)
        assertTrue(down[2].isShiftPressed)
        assertTrue(down[3].isShiftPressed)
        assertTrue(events.none { it.keyCode == KeyEvent.KEYCODE_ENTER })
    }

    @Test fun newlineIsRejectedBeforeClearingOrTypingAnything() {
        val (receiver, server, keyboard) = target()
        assertFalse(receiver.fillSecretText("fixture\nnever-submit"))
        verify(exactly = 0) { server.injectKeyPress(XKeycode.KEY_CTRL_L) }
        verify(exactly = 0) { keyboard.onKeyEvent(any()) }
    }

    @Test fun unsupportedCharacterDoesNotProduceAPartialPassword() {
        val (receiver, server, keyboard) = target()
        assertFalse(receiver.fillSecretText("fixture\u2603"))
        verify(exactly = 0) { server.injectKeyPress(XKeycode.KEY_CTRL_L) }
        verify(exactly = 0) { keyboard.onKeyEvent(any()) }
    }
}
