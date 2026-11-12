package app.gamenative.ui.screen.wow

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** A cleanup from an earlier launch must never delete the next launch's login file. */
internal class TransientLoginFiles {
    private var generation = 0L
    private val active = mutableMapOf<File, Long>()

    @Synchronized fun write(file: File, contents: String) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temporary = File.createTempFile("login-", ".tmp", file.parentFile)
        try {
            temporary.writeText(contents)
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            active[file.canonicalFile] = ++generation
        } finally {
            temporary.delete()
        }
    }

    @Synchronized fun current(file: File): Long? = active[file.canonicalFile]

    @Synchronized fun remove(file: File) {
        active.remove(file.canonicalFile)
        check(!file.exists() || file.delete()) { "Could not remove temporary login file" }
    }

    @Synchronized fun removeIfCurrent(file: File, token: Long): Boolean {
        if (active[file.canonicalFile] != token) return false
        remove(file)
        return true
    }
}
