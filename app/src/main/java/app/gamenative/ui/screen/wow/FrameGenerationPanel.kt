package app.gamenative.ui.screen.wow

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gamenative.ui.theme.WowCream
import app.gamenative.ui.theme.WowError
import app.gamenative.ui.theme.WowGold
import app.gamenative.utils.LsfgDllStore
import app.gamenative.utils.LsfgPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

@Composable
internal fun FrameGenerationPanel(flavor: WowFlavor, enabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imported by remember { mutableStateOf(LsfgDllStore.available(context)) }
    var multiplier by remember(flavor) { mutableIntStateOf(LsfgPreferences.multiplier(context, flavor.name)) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    fun importDll(open: () -> InputStream) {
        busy = true
        failed = false
        message = "Checking Lossless.dll and preparing shaders..."
        scope.launch {
            try {
                withContext(Dispatchers.IO) { open().use { LsfgDllStore.import(context, it) } }
                imported = true
                message = "Lossless.dll imported. The original file is unchanged."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
                message = e.message ?: "Could not import Lossless.dll."
            } finally {
                busy = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importDll {
            checkNotNull(context.contentResolver.openInputStream(uri)) { "Cannot read the selected DLL." }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("LOSSLESS SCALING · FRAME GENERATION", color = WowGold)
        Text(
            if (imported) "Your Lossless.dll is ready. Choose a multiplier for ${flavor.label}."
            else "Import Lossless.dll from your own copy of Lossless Scaling. No DLL is bundled or downloaded.",
            color = WowCream,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = enabled && !busy) {
                Text(if (imported) "Replace DLL" else "Import DLL")
            }
            OutlinedButton(
                onClick = {
                    importDll {
                        val root = Environment.getExternalStorageDirectory()
                        val source = listOf("Bloodborne/Lossless.dll", "Download/Lossless.dll")
                            .map { File(root, it) }.firstOrNull { it.isFile && it.canRead() }
                        checkNotNull(source) { "No readable DLL in Bloodborne or Download. Use Import DLL to choose it." }
                            .inputStream()
                    }
                },
                enabled = enabled && !busy,
            ) { Text("Use existing Thor DLL") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0, 2, 3, 4).forEach { value ->
                FilterChip(
                    selected = multiplier == value,
                    onClick = {
                        multiplier = value
                        LsfgPreferences.save(context, flavor.name, value)
                    },
                    label = { Text(if (value == 0) "Off" else "${value}×") },
                    enabled = enabled && !busy && (imported || value == 0),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text("Start with 2×. In-game LSFG controls include flow scale and performance mode. Changes here apply on the next launch.", color = WowCream)
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = if (failed) WowError else WowGold) }
    }
}
