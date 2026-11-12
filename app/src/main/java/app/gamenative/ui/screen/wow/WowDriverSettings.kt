package app.gamenative.ui.screen.wow

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gamenative.ui.theme.WowBronze
import app.gamenative.ui.theme.WowCream
import app.gamenative.ui.theme.WowError
import app.gamenative.ui.theme.WowFrame
import app.gamenative.ui.theme.WowGold
import app.gamenative.ui.theme.WowMuted
import app.gamenative.ui.theme.WowStoneTop
import app.gamenative.ui.theme.WowSubtle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Which Turnip (Adrenotools) graphics driver the game boots with. By default the launcher picks one
 * for the detected GPU; this lets you pick any installed driver, or import another one from a .zip.
 */
object WowDriverSettings {
    private const val PREFS = "wow_forever"
    private const val KEY_DRIVER = "driver_choice"
    private const val KEY_TU_DEBUG = "tu_debug_override"

    /** A driver that can be picked. [builtIn] ones ship with the app and cannot be removed. */
    data class Entry(val name: String, val version: String, val builtIn: Boolean)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun driversDir(context: Context) = File(context.filesDir, "contents/adrenotools")

    private val builtInNames get() = listOf(ADRENO_8XX_DRIVER, DEFAULT_DRIVER)

    /** The chosen driver name, or empty for automatic. */
    fun choice(context: Context): String = prefs(context).getString(KEY_DRIVER, "").orEmpty()

    fun setChoice(context: Context, name: String) {
        prefs(context).edit().putString(KEY_DRIVER, name).apply()
    }

    /** Custom TU_DEBUG flags, or empty to use the launcher's defaults. */
    fun tuDebug(context: Context): String = prefs(context).getString(KEY_TU_DEBUG, "").orEmpty()

    fun setTuDebug(context: Context, value: String) {
        prefs(context).edit().putString(KEY_TU_DEBUG, value.trim()).apply()
    }

    private fun isInstalled(context: Context, name: String) = File(driversDir(context), "$name/meta.json").isFile

    /** Built-in drivers first, then anything imported. */
    fun entries(context: Context): List<Entry> {
        val imported = driversDir(context).listFiles()
            ?.filter { it.isDirectory && File(it, "meta.json").isFile && it.name !in builtInNames }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
        return builtInNames.map { Entry(it, versionOf(context, it), builtIn = true) } +
            imported.map { Entry(it.name, versionOf(context, it.name), builtIn = false) }
    }

    private fun versionOf(context: Context, name: String): String = try {
        JSONObject(File(driversDir(context), "$name/meta.json").readText()).optString("driverVersion")
    } catch (e: Exception) {
        ""
    }

    /** The profile to boot with: the detected one, with the chosen driver and TU_DEBUG flags applied. */
    internal fun apply(context: Context, detected: GpuProfile): GpuProfile {
        val picked = choice(context)
        val usable = picked.isNotEmpty() && (picked in builtInNames || isInstalled(context, picked))
        val flags = tuDebug(context)
        return detected.copy(
            driver = if (usable) picked else detected.driver,
            tuDebug = flags.ifEmpty { detected.tuDebug },
        )
    }

    /** Remove an imported driver. If it was selected, the choice goes back to automatic. */
    fun remove(context: Context, name: String) {
        if (name in builtInNames) return
        File(driversDir(context), name).deleteRecursively()
        if (choice(context) == name) setChoice(context, "")
    }

    /** Install an Adrenotools driver .zip (a meta.json plus the driver library). Returns the new driver's name. */
    fun import(context: Context, uri: Uri): Result<String> = runCatching {
        val base = driversDir(context)
        base.mkdirs()
        val tmp = File(base, "tmp_import")
        tmp.deleteRecursively()
        tmp.mkdirs()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open that file.")
            input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val out = File(tmp, entry.name)
                        if (!out.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) {
                            throw IOException("That zip has an unsafe file path.")
                        }
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                        }
                        entry = zip.nextEntry
                    }
                }
            }
            val meta = tmp.walkTopDown().firstOrNull { it.isFile && it.name == "meta.json" }
                ?: throw IOException("No meta.json found. This is not an Adrenotools driver zip.")
            val json = JSONObject(meta.readText())
            val library = json.optString("libraryName")
            if (library.isBlank() || !File(meta.parentFile, library).isFile) {
                throw IOException("meta.json points to a driver library that is missing from the zip.")
            }
            val name = json.optString("name").replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_', '.', '-')
            if (name.isEmpty()) throw IOException("meta.json has no usable driver name.")
            if (name in builtInNames || File(base, name).exists()) {
                throw IOException("A driver named $name is already installed. Remove it first to replace it.")
            }
            val driverDir = meta.parentFile ?: throw IOException("Couldn't read the zip.")
            if (!driverDir.renameTo(File(base, name))) throw IOException("Couldn't install the driver.")
            name
        } finally {
            tmp.deleteRecursively()
        }
    }
}

@Composable
fun GraphicsDriverDialog(autoDriver: String, autoFlags: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(WowDriverSettings.entries(context)) }
    var selected by remember { mutableStateOf(WowDriverSettings.choice(context)) }
    var flags by remember { mutableStateOf(WowDriverSettings.tuDebug(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { WowDriverSettings.import(context, uri) }
            importing = false
            entries = WowDriverSettings.entries(context)
            result.onSuccess {
                selected = it
                message = "Installed $it and selected it."
                messageIsError = false
            }.onFailure {
                message = it.message ?: "Couldn't install that driver."
                messageIsError = true
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = WowCream,
        unfocusedTextColor = WowCream,
        focusedBorderColor = WowGold,
        unfocusedBorderColor = WowFrame,
        focusedLabelColor = WowGold,
        unfocusedLabelColor = WowMuted,
        cursorColor = WowGold,
    )

    @Composable
    fun Choice(title: String, detail: String, value: String, onRemove: (() -> Unit)? = null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selected = value },
        ) {
            RadioButton(
                selected = selected == value,
                onClick = { selected = value },
                colors = RadioButtonDefaults.colors(selectedColor = WowGold, unselectedColor = WowMuted),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, color = WowCream, fontWeight = FontWeight.Bold)
                if (detail.isNotEmpty()) Text(detail, fontSize = 11.sp, color = WowSubtle)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove driver", tint = WowMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graphics Driver", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = WowGold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Choice("Auto", "Currently $autoDriver", "")
                entries.forEach { entry ->
                    val detail = listOf(entry.version, if (entry.builtIn) "built in" else "imported")
                        .filter { it.isNotEmpty() }
                        .joinToString(" - ")
                    Choice(
                        title = entry.name,
                        detail = detail,
                        value = entry.name,
                        onRemove = if (entry.builtIn) null else {
                            {
                                WowDriverSettings.remove(context, entry.name)
                                entries = WowDriverSettings.entries(context)
                                if (selected == entry.name) selected = ""
                            }
                        },
                    )
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { zipPicker.launch(arrayOf("*/*")) }, enabled = !importing) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = WowFrame, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (importing) "Importing..." else "Import driver (.zip)", fontSize = 12.sp, color = WowFrame)
                }
                message?.let {
                    Text(it, fontSize = 11.sp, color = if (messageIsError) WowError else WowSubtle, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = flags,
                    onValueChange = { flags = it },
                    label = { Text("TU_DEBUG flags") },
                    placeholder = { Text(autoFlags) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Leave blank to use the default ($autoFlags). Changes apply the next time you launch.",
                    fontSize = 11.sp,
                    color = WowSubtle,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    WowDriverSettings.setChoice(context, selected)
                    WowDriverSettings.setTuDebug(context, flags)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WowBronze, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = WowMuted) }
        },
        containerColor = WowStoneTop,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(3.dp, WowFrame, RoundedCornerShape(10.dp)),
    )
}
