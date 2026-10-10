package app.gamenative.externaldisplay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
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

        section("Modifier keys shown", collapsible = true) {
            addView(toggleRow(listOf("Shift" to PadSettings.MOD_SHIFT, "Ctrl" to PadSettings.MOD_CTRL, "Alt" to PadSettings.MOD_ALT)))
        }
        section("Window buttons shown", collapsible = true) {
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
        section("Pad sections shown", collapsible = true) {
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
            addView(toggleRow(listOf("Swap F-keys and modifier keys" to PadSettings.SWAP_FKEYS_MODS)))
            addView(cycleRow("Hotbar pages", PadSettings.HOTBAR_PAGE, listOf("Off", "Shift + number", "Ctrl + number", "Alt + number")))
        }
        section("Look and feel", collapsible = true) {
            addView(sliderRow("Muted borders", PadSettings.MUTED, 0, 100) { "$it" })
            addView(sliderRow("Number emphasis", PadSettings.EMPHASIS, 0, 200) { "$it%" })
            addView(sliderRow("Label size", PadSettings.LABEL_SCALE, 80, 140) { "$it%" })
            addView(cycleRow("Haptics", PadSettings.HAPTICS, listOf("Off", "Light", "Normal", "Strong")))
            addView(sliderRow("Double-tap lock", PadSettings.DOUBLE_TAP_MS, 200, 600) { "$it ms" })
            addView(sliderRow("Click wait for double tap", PadSettings.CLICK_WAIT_MS, 0, 300) { if (it == 0) "Off" else "$it ms" })
        }
        section("Trackpad", collapsible = true) {
            addView(sliderRow("Speed", PadSettings.TP_SPEED, 1, 20) { tenths(it) })
            addView(sliderRow("Acceleration", PadSettings.TP_ACCEL, 10, 30) { tenths(it) })
            addView(toggleRow(listOf("Tap to click" to PadSettings.TP_TAP)))
            addView(toggleRow(listOf("Scroll area" to PadSettings.SCROLLBAR_ON, "Invert scroll" to PadSettings.SCROLLBAR_INVERT)))
            addView(cycleRow("Scroll area side", PadSettings.SCROLLBAR_SIDE, listOf("Right", "Left")))
            addView(sliderRow("Scroll area speed", PadSettings.SCROLLBAR_SPEED, 1, 10) { "$it" })
        }
        // Only shown when R3 mouse/camera toggling was switched on at launch (main screen).
        if (PadSettings.bool(PadSettings.R3_TOGGLE)) {
            section("Right-stick cursor (R3)", collapsible = true) {
                addView(sliderRow("Base speed", PadSettings.STICK_BASE, 5, 30) { tenths(it) })
                addView(sliderRow("Max speed", PadSettings.STICK_MAX, 10, 50) { tenths(it) })
                addView(sliderRow("Ramp starts at", PadSettings.STICK_RAMP, 50, 99) { "$it%" })
                addView(sliderRow("Stick deadzone", PadSettings.STICK_DEADZONE, 5, 40) { "$it%" })
                addView(cycleRow("A / B in cursor mode", PadSettings.AB_MODE, listOf("Off", "A left, B right", "A right, B left")))
                addView(toggleRow(listOf("Double-tap A / B holds the click" to PadSettings.CLICK_HOLD)))
                addView(sliderRow("Double-click speed", PadSettings.CLICK_HOLD_MS, 150, 600) { "$it ms" })
                addView(toggleRow(listOf("Y / X scroll in cursor mode" to PadSettings.SCROLL_KEYS, "Invert Y / X scroll" to PadSettings.SCROLL_INVERT)))
                addView(sliderRow("Scroll speed", PadSettings.SCROLL_SPEED, 1, 10) { "$it" })
                addView(toggleRow(listOf("Show mode message" to PadSettings.MODE_MESSAGE)))
            }
        }
        section("Game and app", collapsible = true) {
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
        section("Remap, rename, and recolor buttons", collapsible = true) {
            addView(rowOf(button("Choose a button to remap or rename") { showRemap() }))
        }
        section("Profiles", collapsible = true) {
            val status = note("")
            addView(launchProfileRow())
            for (slot in 1..PadSettings.PROFILE_SLOTS) {
                addView(profileRow(slot, status))
            }
            addView(status)
        }
        section("Backup", collapsible = true) {
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
            addView(
                rowOf(
                    button("Export layout to file") {
                        status.text = runCatching {
                            val file = layoutFile()
                            file.writeText(PadSettings.layout().toString(2))
                            "Saved ${file.name} in your WoW folder"
                        }.getOrElse { "Could not save the layout file" }
                    },
                    button("Import layout from file") {
                        val file = layoutFile()
                        val json = runCatching { JSONObject(file.readText()) }.getOrNull()
                        if (json == null || !PadSettings.applyLayout(json)) {
                            status.text = "No layout file found. Looking for ${file.name} in your WoW folder"
                        } else {
                            showMain()
                        }
                    },
                ),
            )
            addView(note("A layout is only the buttons: keys, names, colors and order."))
            addView(status)
        }
        section("Reset", collapsible = true) {
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
        section("Remap, rename, and recolor buttons") {
            addView(note("Tap a pad button, then choose the key it should send, give it a new name, or change its color."))
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
            addView(
                rowOf(
                    button("Reset all colors") {
                        PadSettings.clearColors()
                        showRemap()
                    },
                    button("Reset button order") {
                        PadSettings.clearOrder()
                        showRemap()
                    },
                    filler = 1,
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
                            button(if (mapped == null) shown else "$shown → ${keyName(mapped)}", active = mapped != null || PadSettings.renamed(label) != null || PadSettings.colorOf(label) != null) {
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
        section("Color") {
            val group = ExternalActionBarView.groupOf(label)
            addView(
                rowOf(
                    *COLOR_CHOICES.map { (_, color) ->
                        swatch(color, selected = PadSettings.colorOf(label) == color) {
                            PadSettings.setColor(listOf(label), color)
                            showKeyPicker(label, default)
                        }
                    }.toTypedArray(),
                    swatch(null, selected = PadSettings.colorOf(label) == null) {
                        PadSettings.setColor(listOf(label), null)
                        showKeyPicker(label, default)
                    },
                ),
            )
            if (group != null) {
                addView(
                    rowOf(
                        button("Apply to whole group") {
                            PadSettings.setColor(ExternalActionBarView.defaultLabels(group), PadSettings.colorOf(label))
                            showKeyPicker(label, default)
                        },
                    ),
                )
            }
        }
        ExternalActionBarView.groupOf(label)?.let { group ->
            section("Order in ${GROUP_NAMES[group]}") {
                val defaults = ExternalActionBarView.defaultLabels(group)
                val list = PadSettings.ordered(group, defaults)
                val pos = list.indexOf(label)
                addView(note("Position ${pos + 1} of ${list.size}. A button only moves inside its own group."))
                addView(
                    rowOf(
                        button("\u25B2 Up") {
                            PadSettings.move(group, defaults, label, -1)
                            showKeyPicker(label, default)
                        },
                        button("\u25BC Down") {
                            PadSettings.move(group, defaults, label, 1)
                            showKeyPicker(label, default)
                        },
                    ),
                )
            }
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

    /** A color square for the picker; null is the default gold. A white ring marks the current choice. */
    private fun swatch(color: Int?, selected: Boolean, onClick: () -> Unit): Button = Button(context).apply {
        text = if (color == null) "Default" else ""
        setAllCaps(false)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(theme.text)
        typeface = theme.typeface
        minHeight = dp(36)
        minimumHeight = dp(36)
        setPadding(0, 0, 0, 0)
        background = GradientDrawable().apply {
            cornerRadius = 8 * density
            setColor(color ?: 0xFF28201A.toInt())
            setStroke((if (selected) 3 else 1) * density.toInt().coerceAtLeast(1), if (selected) 0xFFFFFFFF.toInt() else theme.border)
        }
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        setOnClickListener {
            PadSettings.haptic(this)
            onClick()
        }
    }

    // Building blocks

    private fun dp(value: Int): Int = (value * density).toInt()

    private fun section(title: String, collapsible: Boolean = false, build: LinearLayout.() -> Unit) {
        val group = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = theme.groupBackground(density, strong = false)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, 0, 0, dp(6)) }
        }
        val header = TextView(context).apply {
            text = if (collapsible) "\u25B8  ${title.uppercase()}" else title.uppercase()
            setTextColor(theme.border)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (collapsible) 13f else 11f)
            typeface = theme.typeface
            setPadding(dp(8), dp(if (collapsible) 8 else 2), dp(8), dp(if (collapsible) 8 else 2))
        }
        group.addView(header)
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        group.addView(body)
        body.build()
        if (collapsible) {
            // Closed by default; tap the title to open or close.
            body.visibility = View.GONE
            header.setOnClickListener {
                val open = body.visibility != View.VISIBLE
                body.visibility = if (open) View.VISIBLE else View.GONE
                header.text = (if (open) "\u25BE  " else "\u25B8  ") + title.uppercase()
            }
        }
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

    /** Picks the profile this game loads at launch (None keeps the pad as it is). */
    private fun launchProfileRow(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, 0, 0)
        val game = WowFlavor.current
        fun text(slot: Int) = if (slot == 0) "None" else "Profile $slot"
        addView(label("Loads at launch for ${game.label}", widthDp = 150))
        addView(
            button(text(PadSettings.launchProfile(game.name))) { b ->
                val next = (PadSettings.launchProfile(game.name) + 1) % (PadSettings.PROFILE_SLOTS + 1)
                PadSettings.setLaunchProfile(game.name, next)
                b.text = text(next)
            },
        )
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

    /** The layout file sits next to the game data, so it survives reinstalling the app. */
    private fun layoutFile() = File(File(GamePath.load(context)), "WoWPad-layout.json")

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

        private val GROUP_NAMES = mapOf(
            ExternalActionBarView.GROUP_WINDOWS to "the window buttons",
            ExternalActionBarView.GROUP_FKEYS to "the F-keys",
            ExternalActionBarView.GROUP_NUMBERS to "the number block",
        )

        /** Trim colors offered per button; "Default" keeps the gold. */
        private val COLOR_CHOICES = listOf(
            "Red" to 0xFFE53935.toInt(),
            "Orange" to 0xFFFB8C00.toInt(),
            "Yellow" to 0xFFFDD835.toInt(),
            "Green" to 0xFF43A047.toInt(),
            "Blue" to 0xFF1E88E5.toInt(),
            "Purple" to 0xFF8E24AA.toInt(),
            "White" to 0xFFEEEEEE.toInt(),
        )

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
