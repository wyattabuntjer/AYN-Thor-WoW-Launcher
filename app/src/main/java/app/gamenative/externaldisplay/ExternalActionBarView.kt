package app.gamenative.externaldisplay

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.winlator.xserver.XKeycode
import com.winlator.xserver.XServer

/**
 * The main second-screen pad.
 *
 * Left half: a column of window shortcuts (Map, Character, Spellbook, Talents, Skills, Quest Log,
 * Social, System).
 * Right half, top to bottom: the modifier buttons (see [setModifierRow]), the 12 action buttons
 * (4 rows of 3), and F1-F12 in two rows.
 *
 * Every button sends WoW's default key, so the pad works with the stock bindings and needs no addon.
 * To use different keys, change the lists in the companion object.
 *
 * One touch is exactly one key press and one key release. There are no macros, sequences or timed
 * repeats here: anything multi-step belongs in WoW's own macro system.
 */
class ExternalActionBarView(
    context: Context,
    private val xServer: XServer,
    private val theme: PadTheme,
    /** Called after a button's key has been released, e.g. so one-shot modifiers can let go. */
    private val onKeyTapped: () -> Unit = {},
) : LinearLayout(context) {

    private data class Slot(val label: String, val key: XKeycode)

    private val downKeys = mutableSetOf<XKeycode>()
    private val rightColumn: LinearLayout
    private val modifierGroup: LinearLayout

    /** A backing panel that hides itself while it has nothing in it (e.g. the modifiers moved to the trackpad). */
    private class Group(context: Context) : LinearLayout(context) {
        override fun onViewAdded(child: View?) {
            super.onViewAdded(child)
            visibility = VISIBLE
        }

        override fun onViewRemoved(child: View?) {
            super.onViewRemoved(child)
            if (childCount == 0) visibility = GONE
        }
    }

    init {
        orientation = HORIZONTAL
        isMotionEventSplittingEnabled = true
        setBackgroundColor(theme.background)
        val pad = dp(8)
        setPadding(pad, pad, pad, pad)

        // Each group of buttons sits on its own faint backing; the number block's is the strongest.
        // Window buttons: one column up to SINGLE_COLUMN_MAX buttons, two columns beyond that.
        val windows = visibleWindows()
        val columns = if (windows.size > SINGLE_COLUMN_MAX) windows.chunked((windows.size + 1) / 2) else listOf(windows)
        val split = columns.size > 1
        val leftColumn = LinearLayout(context).apply {
            orientation = HORIZONTAL
            isMotionEventSplittingEnabled = true
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, if (split) SPLIT_LEFT_WEIGHT else LEFT_WEIGHT)
            columns.forEach { slots ->
                addView(
                    group(vertical = true, strong = false).apply {
                        layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                        slots.forEach { addView(keyRow(listOf(it), textSp = if (split) 13f else 15f, weight = 1f, muted = true)) }
                    },
                )
            }
            if (windows.isEmpty() || !PadSettings.bool(PadSettings.SEC_WINDOWS)) visibility = GONE
        }
        modifierGroup = group(vertical = false, strong = false).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.7f).apply { setMargins(0, dp(3), 0, dp(3)) }
            visibility = GONE
        }
        val numberGroup = group(vertical = true, strong = true).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 4f).apply { setMargins(0, dp(3), 0, dp(3)) }
            ACTION_SLOTS.chunked(3).forEach {
                addView(keyRow(it, textSp = 26f, weight = 1f, raised = PadSettings.emphasis > 0f, hotbar = true))
            }
            if (!PadSettings.bool(PadSettings.SEC_NUMBERS)) visibility = GONE
        }
        val functionGroup = group(vertical = true, strong = false).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.2f).apply { setMargins(0, dp(3), 0, dp(3)) }
            // F1 up to the chosen count (3-12). Up to six sit in one row of big buttons; more split into two
            // balanced rows, so the group always fills the same space.
            val count = PadSettings.int(PadSettings.FKEY_N).coerceIn(3, 12)
            val fKeys = FUNCTION_KEYS.take(count)
            val oneRow = count <= 6
            fKeys.chunked(if (oneRow) count else (count + 1) / 2)
                .forEach { addView(keyRow(it, textSp = if (oneRow) 16f else 13f, weight = 1f, muted = true)) }
            if (!PadSettings.bool(PadSettings.SEC_FKEYS)) visibility = GONE
        }
        rightColumn = column(RIGHT_WEIGHT).apply {
            // "Swap F-keys and modifiers" puts the F-keys on top and the modifier keys at the bottom.
            if (PadSettings.bool(PadSettings.SWAP_FKEYS_MODS)) {
                addView(functionGroup)
                addView(numberGroup)
                addView(modifierGroup)
            } else {
                addView(modifierGroup)
                addView(numberGroup)
                addView(functionGroup)
            }
        }
        // "Swap sides" puts the window column on the right.
        if (PadSettings.bool(PadSettings.SWAP_SIDES)) {
            addView(rightColumn)
            addView(leftColumn)
        } else {
            addView(leftColumn)
            addView(rightColumn)
        }
    }

    /** The window buttons to show: switched on in settings. Every game shows them all, since any button can be renamed. */
    private fun visibleWindows(): List<Slot> = PANELS.filter { slot ->
        PadSettings.bool(PadSettings.windowKey(slot.label))
    }

    /** Puts the modifier buttons above the action buttons. */
    fun setModifierRow(row: View) {
        (row.parent as? android.view.ViewGroup)?.removeView(row)
        row.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        modifierGroup.addView(row)
    }

    private fun group(vertical: Boolean, strong: Boolean): LinearLayout = Group(context).apply {
        orientation = if (vertical) VERTICAL else HORIZONTAL
        isMotionEventSplittingEnabled = true
        background = theme.groupBackground(resources.displayMetrics.density, strong)
        setPadding(dp(4), dp(4), dp(4), dp(4))
    }

    private fun column(weight: Float) = LinearLayout(context).apply {
        orientation = VERTICAL
        isMotionEventSplittingEnabled = true
        layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
    }

    /** [muted] dims the button trim; the action buttons (1-9, 0, -, =) are left at full strength. */
    private fun keyRow(
        slots: List<Slot>,
        textSp: Float,
        weight: Float,
        muted: Boolean = false,
        raised: Boolean = false,
        hotbar: Boolean = false,
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            isMotionEventSplittingEnabled = true
            // Rows share the column's height by weight, so everything always fits.
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, weight)
            slots.forEach { addView(createButton(it, textSp, muted, raised, hotbar)) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createButton(slot: Slot, textSp: Float, muted: Boolean, raised: Boolean, hotbar: Boolean): View {
        return TextView(context).apply {
            text = PadSettings.displayName(slot.label)
            contentDescription = slot.label
            gravity = Gravity.CENTER
            setTextColor(theme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp * PadSettings.labelScale)
            typeface = theme.typeface
            maxLines = 1
            background = createKeyBackground(muted, raised)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                val margin = dp(3)
                setMargins(margin, margin, margin, margin)
            }
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.isPressed = true
                        PadSettings.haptic(view)
                        pressKey(keyFor(slot), if (hotbar) hotbarModifier() else null)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.isPressed = false
                        releaseKey(keyFor(slot))
                    }
                }
                true
            }
        }
    }

    /** The key a button sends: the user's remap if there is one, otherwise the default. */
    private fun keyFor(slot: Slot): XKeycode =
        PadSettings.remapped(slot.label)?.let { name -> runCatching { XKeycode.valueOf(name) }.getOrNull() } ?: slot.key

    /** "Hotbar page": the number buttons also hold Shift, Ctrl or Alt, to reach the other action bars. */
    private fun hotbarModifier(): XKeycode? = when (PadSettings.int(PadSettings.HOTBAR_PAGE)) {
        1 -> XKeycode.KEY_SHIFT_L
        2 -> XKeycode.KEY_CTRL_L
        3 -> XKeycode.KEY_ALT_L
        else -> null
    }

    private val pageModifiers = mutableMapOf<XKeycode, XKeycode>()

    private fun pressKey(key: XKeycode, pageModifier: XKeycode? = null) {
        if (!downKeys.add(key)) return
        if (pageModifier != null) {
            pageModifiers[key] = pageModifier
            xServer.injectKeyPress(pageModifier)
        }
        xServer.injectKeyPress(key)
    }

    private fun releaseKey(key: XKeycode) {
        if (!downKeys.remove(key)) return
        xServer.injectKeyRelease(key)
        pageModifiers.remove(key)?.let { xServer.injectKeyRelease(it) }
        onKeyTapped()
    }

    private fun createKeyBackground(muted: Boolean, raised: Boolean) =
        theme.buttonStates(resources.displayMetrics.density, 10f, muted, raised)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Never leave a key stuck down if the display goes away or the game closes mid-press. */
    override fun onDetachedFromWindow() {
        downKeys.toList().forEach { xServer.injectKeyRelease(it) }
        pageModifiers.values.forEach { xServer.injectKeyRelease(it) }
        pageModifiers.clear()
        downKeys.clear()
        super.onDetachedFromWindow()
    }

    companion object {
        /** Every remappable pad button: its label and the key it sends by default. */
        /** Window buttons that exist in the selected client, for the "Window buttons shown" settings. */
        fun availableWindowLabels(): List<String> =
            PANELS.map { it.label }

        fun remappableButtons(): List<Pair<String, XKeycode>> =
            (PANELS + ACTION_SLOTS + FUNCTION_KEYS).map { it.label to it.key }

        // Width split between the window-shortcut column and the F-key/number/modifier block.
        // A smaller RIGHT_WEIGHT squeezes that block toward the right edge, within reach of a right thumb.
        private const val LEFT_WEIGHT = 1f
        private const val RIGHT_WEIGHT = 1f

        // Up to this many window buttons stay in one column; more split it in two, a little wider overall.
        private const val SINGLE_COLUMN_MAX = 8
        private const val SPLIT_LEFT_WEIGHT = 1.45f

        // WoW's default bindings for the character, spellbook and similar windows.
        private val PANELS = listOf(
            Slot("Map", XKeycode.KEY_M),
            Slot("Character", XKeycode.KEY_C),
            Slot("Spellbook", XKeycode.KEY_P),
            Slot("Talents", XKeycode.KEY_N),
            Slot("Skills", XKeycode.KEY_K),
            Slot("Quest Log", XKeycode.KEY_L),
            Slot("Social", XKeycode.KEY_O),
            // Escape opens the game menu ("System") when nothing else is open.
            Slot("System", XKeycode.KEY_ESC),
            Slot("Bags", XKeycode.KEY_B),
            Slot("Group Finder", XKeycode.KEY_I),
            Slot("Achievements", XKeycode.KEY_Y),
            Slot("Guild", XKeycode.KEY_J),
        )

        private val FUNCTION_KEYS = listOf(
            Slot("F1", XKeycode.KEY_F1),
            Slot("F2", XKeycode.KEY_F2),
            Slot("F3", XKeycode.KEY_F3),
            Slot("F4", XKeycode.KEY_F4),
            Slot("F5", XKeycode.KEY_F5),
            Slot("F6", XKeycode.KEY_F6),
            Slot("F7", XKeycode.KEY_F7),
            Slot("F8", XKeycode.KEY_F8),
            Slot("F9", XKeycode.KEY_F9),
            Slot("F10", XKeycode.KEY_F10),
            Slot("F11", XKeycode.KEY_F11),
            Slot("F12", XKeycode.KEY_F12),
        )

        // WoW's default bindings for Action Bar 1, slots 1 to 12.
        private val ACTION_SLOTS = listOf(
            Slot("1", XKeycode.KEY_1),
            Slot("2", XKeycode.KEY_2),
            Slot("3", XKeycode.KEY_3),
            Slot("4", XKeycode.KEY_4),
            Slot("5", XKeycode.KEY_5),
            Slot("6", XKeycode.KEY_6),
            Slot("7", XKeycode.KEY_7),
            Slot("8", XKeycode.KEY_8),
            Slot("9", XKeycode.KEY_9),
            Slot("0", XKeycode.KEY_0),
            Slot("-", XKeycode.KEY_MINUS),
            Slot("=", XKeycode.KEY_EQUAL),
        )
    }
}
