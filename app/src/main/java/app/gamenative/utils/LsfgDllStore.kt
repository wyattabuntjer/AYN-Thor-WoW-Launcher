package app.gamenative.utils

import android.content.Context
import com.winlator.renderer.lsfg.LosslessScaling
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Owns an app-private copy; never modifies the user's source DLL. */
object LsfgDllStore {
    private const val MAX_BYTES = 128L * 1024 * 1024

    fun dll(context: Context): File = File(context.filesDir, "lsfg/Lossless.dll")

    fun available(context: Context): Boolean = dll(context).isFile

    fun import(context: Context, input: InputStream): File =
        importTo(dll(context), input) { candidate ->
            checkNotNull(LosslessScaling.resolveOrBuildCache(context, candidate, null)) {
                "This Lossless.dll is not supported by the bundled LSFG shader reader. The previous DLL was kept."
            }
        }

    /** Stage and validate before replacing a working DLL, including on read/parse failure. */
    internal fun importTo(destination: File, input: InputStream, validate: (File) -> Unit): File {
        check(destination.parentFile!!.isDirectory || destination.parentFile!!.mkdirs()) {
            "Cannot create the Lossless Scaling folder."
        }
        val staging = File.createTempFile("lossless-", ".import", destination.parentFile)
        try {
            staging.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES) { "Lossless.dll is larger than 128 MB." }
                    output.write(buffer, 0, count)
                }
            }
            requirePe(staging)
            validate(staging)
            Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return destination
        } finally {
            staging.delete()
        }
    }

    internal fun requirePe(file: File) {
        require(file.length() >= 64) { "Select a complete Windows Lossless.dll file." }
        RandomAccessFile(file, "r").use { source ->
            require(source.readUnsignedByte() == 0x4d && source.readUnsignedByte() == 0x5a) {
                "The selected file is not a Windows DLL."
            }
            source.seek(0x3c)
            val offset = Integer.reverseBytes(source.readInt()).toLong() and 0xffffffffL
            require(offset >= 64 && offset <= source.length() - 4) { "The DLL has an invalid PE header." }
            source.seek(offset)
            require(source.readInt() == 0x50450000) { "The DLL has an invalid PE signature." }
        }
    }

    internal fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    internal fun sameContent(first: File, second: File): Boolean =
        first.isFile && second.isFile && first.length() == second.length() && sha256(first) == sha256(second)
}
