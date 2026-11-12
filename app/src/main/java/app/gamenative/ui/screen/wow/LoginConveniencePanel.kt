package app.gamenative.ui.screen.wow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gamenative.ui.theme.WowCream
import app.gamenative.ui.theme.WowGold

@Composable
internal fun LoginConveniencePanel(hasSavedLogin: Boolean, enabled: Boolean) {
    val context = LocalContext.current
    var keepRunning by remember { mutableStateOf(WoWSessionSettings.keepRunning(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SIGN-IN CONVENIENCE", color = WowGold)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Keep WoW running in menus and the background", modifier = Modifier.weight(1f), color = WowCream)
            Switch(checked = keepRunning, enabled = enabled, onCheckedChange = {
                keepRunning = it
                WoWSessionSettings.setKeepRunning(context, it)
            })
        }
        Text("Applies next launch. Keeping the game running uses more battery. Network changes and server idle limits can still disconnect it.", color = WowCream)
        OutlinedButton(onClick = { SavedLoginTools.show(context) }, enabled = enabled && hasSavedLogin) {
            Text("Saved password")
        }
        Text("In-game: open the quick menu → Controller → Saved login to fill the selected password box. Authenticator codes are still entered by hand.", color = WowCream)
    }
}
