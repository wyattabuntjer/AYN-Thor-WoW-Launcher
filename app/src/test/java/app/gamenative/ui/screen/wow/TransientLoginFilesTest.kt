package app.gamenative.ui.screen.wow

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TransientLoginFilesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun previousLaunchCleanupCannotDeleteNewCredentials() {
        val files = TransientLoginFiles()
        val target = File(folder.root, "login.txt")
        files.write(target, "old-fixture")
        val old = files.current(target)!!
        files.write(target, "new-fixture")
        val current = files.current(target)!!
        assertFalse(files.removeIfCurrent(target, old))
        assertEquals("new-fixture", target.readText())
        assertTrue(files.removeIfCurrent(target, current))
        assertFalse(target.exists())
    }

    @Test fun flavorSwitchDoesNotRedirectCleanupToAnotherGame() {
        val files = TransientLoginFiles()
        val forever = File(folder.root, "forever/login.txt")
        val retail = File(folder.root, "retail/login.txt")
        files.write(forever, "forever-fixture")
        val token = files.current(forever)!!
        files.write(retail, "retail-fixture")
        assertTrue(files.removeIfCurrent(forever, token))
        assertEquals("retail-fixture", retail.readText())
    }

    @Test fun alternatePathToSameFileDoesNotBypassGenerationGuard() {
        val files = TransientLoginFiles()
        val target = File(folder.root, "forever/login.txt")
        files.write(target, "old-fixture")
        val old = files.current(target)!!
        files.write(File(folder.root, "forever/../forever/login.txt"), "new-fixture")
        assertFalse(files.removeIfCurrent(target, old))
        assertEquals("new-fixture", target.readText())
    }

    @Test fun forgottenFileCannotBeRemovedAgainByStaleCleanup() {
        val files = TransientLoginFiles()
        val target = File(folder.root, "login.txt")
        files.write(target, "fixture")
        val old = files.current(target)!!
        files.remove(target)
        target.writeText("unrelated-new-file")
        assertFalse(files.removeIfCurrent(target, old))
        assertEquals("unrelated-new-file", target.readText())
    }
}
