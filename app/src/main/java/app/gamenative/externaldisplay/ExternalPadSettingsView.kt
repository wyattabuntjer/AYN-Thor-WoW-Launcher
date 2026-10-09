package app.gamenative.externaldisplay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import app.gamenative.ui.screen.wow.GamePath
import app.gamenative.ui.screen.wow.WowFlavor
import com.winlator.xserver.XKeycode
import org.json.JSONObject
import java.io.File

/**
 * The settings screen of the second-screen pad. Everything is written to [PadSettings] as soon as it
 * changes. Layout and look changes show up on the pad itself when this screen is closed.
 *
 * Built in code like the rest of the pad, with the same gold-on-dark look.
 */
class ExternalPadSettingsView(
    context: Context,
    private val theme: PadTheme,
) : ScrollView(context) {

    /** Set by the pad: shows the on-screen keyboard to type text (prompt, starting text, max length, result). */
    var onRequestText: ((String, String, Int, (String) -> Unit) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    init {
        setBackgroundColor(theme.background)
        isFillViewport = false
        val side = dp(8)
        content.setPadding(side, dp(4), side, side)
        addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        showMain()
    }

    // Screens

    private fun showMain() {
        content.removeAllViews()
        scrollTo(0, 0)

        section("Modifier keys shown") {
            addView(toggleRow(listOf("Shift" to PadSettings.MOD_SHIFT, "Ctrl" to PadSettings.MOD_CTRL, "Alt" to PadSettings.MOD_ALT)))
        }
        section("Window buttons shown") {
            addView(note("Up to 8 stay in one column. More split it in two."))
            ExternalActionBarView.availableWindowLabels().chunked(3).forEach { chunk ->
                addView(
                    rowOf(
                        *chunk.map { label ->
                            button(label, active = PadSettings.bool(PadSettings.windowKey(label))) { b ->
                                val on = !PadSettings.bool(PadSettings.windowKey(label))
                                PadSettings.set(PadSettings.windowKey(label), on)
                                style(b, on)
                            }
                        }.toTypedArray(),
                        filler = 3 - chunk.size,
                    ),
                )
            }
        }
        section("Pad sections shown") {
            addView(
                toggleRow(
                    listOf(
                        "Windows" to PadSettings.SEC_WINDOWS,
                        "1-9 0 - =" to PadSettings.SEC_NUMBERS,
                        "F-keys" to PadSettings.SEC_FKEYS,
                    ),
                ),
            )
            addView(sliderRow("F-keys shown", PadSettings.FKEY_N, 3, 12) { "F1-F$it" })
            addView(toggleRow(listOf("Swap sides (windows on the right)" to PadSettings.SWAP_SIDES)))
            addView(cycleRow("Hotbar pages", PadSettings.HOTBAR_PAGE, listOf("Off", "Shift + number", "Ctrl + number", "Alt + number")))
        }
        section("Look and feel") {
            addView(sliderRow("Muted borders", PadSettings.MUTED, 0, 100) { "$it" })
            addView(sliderRow("Number emphasis", PadSettings.EMPHASIS, 0, 200) { "$it%" })
            addView(sliderRow("Label size", PadSettings.LABEL_SCALE, 80, 140) { "$it%" })
            addView(cycleRow("Haptics", PadSettings.HAPTICS, listOf("Off", "Light", "Normal", "Strong")))
            addView(sliderRow("Double-tap lock", PadSettings.DOUBLE_TAP_MS, 200, 600) { "$it ms" })
        }
        section("Trackpad") {
            addView(sliderRow("Speed", PadSettings.TP_SPEED, 1, 20) { tenths(it) })
            addView(sliderRow("Acceleration", PadSettings.TP_ACCEL, 10, 30) { tenths(it) })
            addView(toggleRow(listOf("Tap to click" to PadSettings.TP_TAP)))
            addView(toggleRow(listOf("Scroll area" to PadSettings.SCROLLBAR_ON, "Invert scroll" to PadSettings.SCROLLBAR_INVERT)))
            addView(cycleRow("Scroll area side", PadSettings.SCROLLBAR_SIDE, listOf("Right", "Left")))
            addView(sliderRow("Scroll area speed", PadSettings.SCROLLBAR_SPEED, 1, 10) { "$it" })
        }
        section("Right-stick cursor (R3)") {
            addView(toggleRow(listOf("R3 toggles cursor mode" to PadSettings.R3_TOGGLE)))
            addView(sliderRow("Base speed", PadSettings.STICK_BASE, 5, 30) { tenths(it) })
            addView(sliderRow("Max speed", PadSettings.STICK_MAX, 10, 50) { tenths(it) })
            addView(sliderRow("Ramp starts at", PadSettings.STICK_RAMP, 50, 99) { "$it%" })
            addView(sliderRow("Stick deadzone", PadSettings.STICK_DEADZONE, 5, 40) { "$it%" })
            addView(cycleRow("A / B in cursor mode", PadSettings.AB_MODE, listOf("Off", "A left, B right", "A right, B left")))
            addView(toggleRow(listOf("Y / X scroll in cursor mode" to PadSettings.SCROLL_KEYS, "Invert Y / X scroll" to PadSettings.SCROLL_INVERT)))
            addView(sliderRow("Scroll speed", PadSettings.SCROLL_SPEED, 1, 10) { "$it" })
            addView(toggleRow(listOf("Show mode message" to PadSettings.MODE_MESSAGE)))
        }
        section("Game and app") {
            addView(flavorRow())
            val status = note("")
            addView(
                rowOf(
                    button("Write gamepad cursor lines to Config.wtf") {
                        status.text = writeGamepadConfig()
                    },
                ),
            )
            addView(status)
        }
        section("Remap and rename buttons") {
            addView(rowOf(button("Choose a button to remap or rename") { showRemap() }))
        }
        section("Profiles") {
            val status = note("")
            for (slot in 1..PadSettings.PROFILE_SLOTS) {
                addView(profileRow(slot, status))
            }
            addView(status)
        }
        section("Backup") {
            val status = note("")
            addView(
                rowOf(
                    button("Copy settings") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("pad settings", PadSettings.snapshot().toString()))
                        status.text = "Settings copied to the clipboard"
                    },
                    button("Paste settings") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                        val json = text?.let { runCatching { JSONObject(it) }.getOrNull() }
                        if (json == null) {
                            status.text = "The clipboard has no copied settings"
                        } else {
                            PadSettings.applySnapshot(json)
                            showMain()
                        }
                    },
                ),
            )
            addView(status)
        }
        section("Reset") {
            addView(
                rowOf(
                    button("Reset to defaults") {
                        PadSettings.resetAll()
                        showMain()
                    },
                ),
            )
        }
    }

    private fun showRemap() {
        content.removeAllViews()
        scrollTo(0, 0)
        section("Remap and rename buttons") {
            addView(note("Tap a pad button, then choose the key it should send or give it a new name."))
            addView(
                rowOf(
                    button("Back") { showMain() },
                    button("Reset all remaps") {
                        PadSettings.clearRemaps()
                        showRemap()
                    },
                    button("Reset all names") {
                        PadSettings.clearRenames()
                        showRemap()
                    },
                ),
            )
        }
        val buttons = ExternalActionBarView.remappableButtons()
        section("Pad buttons") {
            buttons.chunked(4).forEach { chunk ->
                addView(
                    rowOf(
                        *chunk.map { (label, default) ->
                            val mapped = PadSettings.remapped(label)
                            val shown = PadSettings.displayName(label)
                            button(if (mapped == null) shown else "$shown → ${keyName(mapped)}", active = mapped != null || PadSettings.renamed(label) != null) {
                                showKeyPicker(label, default)
                            }
                        }.toTypedArray(),
                        filler = 4 - chunk.size,
                    ),
                )
            }
        }
    }

    private fun showKeyPicker(label: String, default: XKeycode) {
        content.removeAllViews()
        scrollTo(0, 0)
        section("Key for \"${PadSettings.displayName(label)}\"") {
            addView(
                rowOf(
                    button("Back") { showRemap() },
                    button("Rename", active = PadSettings.renamed(label) != null) {
                        onRequestText?.invoke("Name for \"$label\"", PadSettings.renamed(label) ?: "", MAX_NAME) { text ->
                            PadSettings.setRename(label, text)
                            showKeyPicker(label, default)
                        }
                    },
                    button("Original name") {
                        PadSettings.setRename(label, null)
                        showKeyPicker(label, default)
                    },
                    button("Default (${keyName(default.name)})") {
                        PadSettings.setRemap(label, null)
                        showRemap()
                    },
                ),
            )
        }
        section("Keys") {
            PICKER_KEYS.chunked(6).forEach { chunk ->
                addView(
                    rowOf(
                        *chunk.map { key ->
                            button(keyName(key.name), active = PadSettings.remapped(label) == key.name) {
                                PadSettings.setRemap(label, key.name)
                                showRemap()
                            }
                        }.toTypedArray(),
                        filler = 6 - chunk.size,
                    ),
                )
            }
        }
    }

    // Building blocks

    private fun dp(value: Int): Int = (value * density).toInt()

    private fun section(title: String, build: LinearLayout.() -> Unit) {
        val group = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.groupBackground(density, strong = false)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, 0, 0, dp(6)) }
        }
        group.addView(
            TextView(context).apply {
                text = title.uppercase()
                setTextColor(theme.border)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = theme.typeface
                setPadding(dp(8), dp(2), dp(8), dp(2))
            },
        )
        group.build()
        content.addView(group)
    }

    private fun note(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(theme.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = theme.typeface
        setPadding(dp(8), dp(2), dp(8), dp(2))
    }

    private fun label(text: String, widthDp: Int = 120) = TextView(context).apply {
        this.text = text
        setTextColor(theme.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = theme.typeface
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun style(button: Button, active: Boolean) {
        button.background = theme.buttonBackground(density, 8f, active, muted = true)
        button.setTextColor(if (active) theme.textPressed else theme.text)
    }

    private fun button(text: String, active: Boolean = false, onClick: (Button) -> Unit): Button = Button(context).apply {
        this.text = text
        setAllCaps(false)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = theme.typeface
        minHeight = dp(36)
        minimumHeight = dp(36)
        setPadding(dp(6), dp(2), dp(6), dp(2))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        style(this, active)
        setOnClickListener {
            PadSettings.haptic(this)
            onClick(this)
        }
    }

    /** A row of equal-width views. [filler] adds empty cells so a short last row keeps the grid widths. */
    private fun rowOf(vararg views: View, filler: Int = 0): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEach { addView(it) }
        repeat(filler) {
            addView(View(context), LinearLayout.LayoutParams(0, 1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
    }

    private fun toggleRow(items: List<Pair<String, String>>): LinearLayout = rowOf(
        *items.map { (text, key) ->
            button(text, active = PadSettings.bool(key)) { b ->
                val on = !PadSettings.bool(key)
                PadSettings.set(key, on)
                style(b, on)
            }
        }.toTypedArray(),
    )

    private fun cycleRow(title: String, key: String, options: List<String>): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, 0, 0)
        addView(label(title))
        addView(
            button(options[PadSettings.int(key).coerceIn(0, options.lastIndex)]) { b ->
                val next = (PadSettings.int(key) + 1) % options.size
                PadSettings.set(key, next)
                b.text = options[next]
            },
        )
    }

    private fun sliderRow(title: String, key: String, min: Int, max: Int, format: (Int) -> String): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
            addView(label(title))
            val valueText = TextView(context).apply {
                setTextColor(theme.text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = theme.typeface
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val current = PadSettings.int(key).coerceIn(min, max)
            valueText.text = format(current)
            val bar = SeekBar(context).apply {
                this.max = max - min
                progress = current - min
                progressTintList = ColorStateList.valueOf(theme.border)
                thumbTintList = ColorStateList.valueOf(theme.borderBright)
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        valueText.text = format(min + progress)
                        if (fromUser) PadSettings.set(key, min + progress)
                    }

                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
            }
            addView(bar)
            addView(valueText)
        }

    private fun profileRow(slot: Int, status: TextView): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, 0, 0)
        val name = label("", widthDp = 150)
        addView(name)
        lateinit var saveButton: Button
        // "Save" for an empty slot, "Overwrite" once a profile is stored there.
        fun refresh() {
            val used = PadSettings.hasProfile(slot)
            name.text = if (used) "Profile $slot (saved)" else "Profile $slot (empty)"
            saveButton.text = if (used) "Overwrite" else "Save"
        }
        saveButton = button("Save") {
            val replaced = PadSettings.hasProfile(slot)
            PadSettings.saveProfile(slot)
            status.text = if (replaced) "Profile $slot overwritten" else "Saved to profile $slot"
            refresh()
        }
        addView(saveButton)
        addView(
            button("Load") {
                if (PadSettings.loadProfile(slot)) showMain() else status.text = "Profile $slot is empty"
            },
        )
        refresh()
    }

    private fun flavorRow(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, 0, 0)
        addView(label("Next launch"))
        addView(
            button(WowFlavor.current.label) { b ->
                val all = WowFlavor.entries
                val next = all[(all.indexOf(WowFlavor.current) + 1) % all.size]
                WowFlavor.select(context, next)
                b.text = next.label
            },
        )
    }

    // Helpers

    private fun tenths(value: Int) = "${value / 10}.${value % 10}"

    private fun keyName(enumName: String): String = when (val n = enumName.removePrefix("KEY_")) {
        "PRIOR" -> "PgUp"
        "NEXT" -> "PgDn"
        "BKSP" -> "Bksp"
        "BRACKET_LEFT" -> "["
        "BRACKET_RIGHT" -> "]"
        "SEMICOLON" -> ";"
        "APOSTROPHE" -> "'"
        "COMMA" -> ","
        "PERIOD" -> "."
        "SLASH" -> "/"
        "BACKSLASH" -> "\\"
        "GRAVE" -> "`"
        "MINUS" -> "-"
        "EQUAL" -> "="
        else -> n.lowercase().replaceFirstChar { it.uppercase() }.takeIf { n.length > 1 } ?: n
    }

    /**
     * Makes sure the three cursor lines are in the current client's Config.wtf. WoW reads that file
     * when it starts, so this applies from the next launch.
     */
    private fun writeGamepadConfig(): String {
        val flavor = WowFlavor.current
        val file = File(File(GamePath.load(context)), "${flavor.dir}/WTF/Config.wtf")
        if (!file.exists()) return "No Config.wtf for ${flavor.label} yet. Launch the game once first."
        return runCatching {
            val wanted = linkedMapOf(
                "GamePadCursorAutoEnable" to "\"0\"",
                "GamePadCursorLeftClick" to "\"PAD1\"",
                "GamePadCursorRightClick" to "\"PAD2\"",
            )
            val seen = mutableSetOf<String>()
            val lines = file.readLines().map { line ->
                val key = line.takeIf { it.startsWith("SET ", ignoreCase = true) }?.removePrefix("SET ")?.trim()?.substringBefore(' ')
                val match = wanted.keys.firstOrNull { it.equals(key, ignoreCase = true) }
                if (match != null) {
                    seen.add(match)
                    "SET $match ${wanted.getValue(match)}"
                } else {
                    line
                }
            } + wanted.filterKeys { it !in seen }.map { (k, v) -> "SET $k $v" }
            file.writeText(lines.joinToString("\n") + "\n")
            "Written for ${flavor.label}. Applies next launch."
        }.getOrElse { "Couldn't write Config.wtf: ${it.message}" }
    }

    private companion object {
        private const val MAX_NAME = 12

        val PICKER_KEYS: List<XKeycode> = buildList {
            val names = ('A'..'Z').map { "KEY_$it" } + (0..9).map { "KEY_$it" } + (1..12).map { "KEY_F$it" } +
                listOf(
                    "KEY_ESC", "KEY_TAB", "KEY_SPACE", "KEY_ENTER", "KEY_BKSP", "KEY_DEL", "KEY_INSERT", "KEY_HOME",
                    "KEY_END", "KEY_PRIOR", "KEY_NEXT", "KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT", "KEY_MINUS",
                    "KEY_EQUAL", "KEY_BRACKET_LEFT", "KEY_BRACKET_RIGHT", "KEY_SEMICOLON", "KEY_APOSTROPHE", "KEY_COMMA",
                    "KEY_PERIOD", "KEY_SLASH", "KEY_BACKSLASH", "KEY_GRAVE",
                )
            names.forEach { name -> runCatching { XKeycode.valueOf(name) }.getOrNull()?.let { add(it) } }
        }
    }
}
