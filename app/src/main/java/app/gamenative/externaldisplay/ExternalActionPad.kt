package app.gamenative.externaldisplay

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import android.util.TypedValue
import android.widget.TextView
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.os.SystemClock
import com.winlator.widget.TouchpadView
import com.winlator.winhandler.MouseEventFlags
import com.winlator.xserver.Pointer
import com.winlator.xserver.XKeycode
import com.winlator.xserver.XServer
import app.gamenative.R
import app.gamenative.data.TouchGestureConfig

/**
 * Whole second-screen UI: a slim header with a trackpad button (left) and a keyboard button (right),
 * and below it either the 12 action buttons or a laptop-style trackpad. The keyboard is the app's own
 * on-screen keyboard, drawn over the bottom of whichever is showing.
 */
class ExternalActionPad(
    context: Context,
    private val xServer: XServer,
    private val theme: PadTheme,
    private val touchpadViewProvider: () -> TouchpadView?,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private lateinit var trackpadButton: ImageButton
    private lateinit var keyboardButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var settingsView: ExternalPadSettingsView
    private lateinit var keyboardView: ExternalOnScreenKeyboardView
    private lateinit var padView: ExternalActionBarView
    private var settingsOpen = false
    private var settingsVersion = 0
    private val heldModifiers = mutableSetOf<XKeycode>()
    private val lockedModifiers = mutableSetOf<XKeycode>()
    // Each modifier can have a button on the pad and one on the trackpad; both follow the same state.
    private val modifierStyles = mutableMapOf<XKeycode, MutableList<(Boolean) -> Unit>>()
    private lateinit var trackpadView: TouchpadView
    private lateinit var trackpadPanel: LinearLayout
    private lateinit var padModifierRow: LinearLayout
    private lateinit var trackpadModifierRow: LinearLayout
    private val releaseMouseButtons = mutableListOf<() -> Unit>()

    init {
        PadSettings.init(context)
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        buildUi()
    }

    /** Builds (or rebuilds, after settings changed) everything below the pad background. */
    private fun buildUi() {
        removeAllViews()
        // Let go of anything still held (including locked modifiers) before the old buttons are dropped.
        heldModifiers.toList().forEach { xServer.injectKeyRelease(it) }
        releaseMouseButtons.forEach { it() }
        heldModifiers.clear()
        lockedModifiers.clear()
        modifierStyles.clear()
        releaseMouseButtons.clear()
        settingsOpen = false

        padView = ExternalActionBarView(context, xServer, theme, onKeyTapped = { releaseModifiers() }).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        trackpadView = TouchpadView(context, xServer, false).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            background = theme.buttonBackground(density, 16f, active = false)
            touchpadViewProvider()?.let { setSimTouchScreen(it.isSimTouchScreen) }
            // Slower, steadier cursor for precise aiming: no speed-up on fast swipes, and a lower base speed.
            setCursorAcceleration(PadSettings.trackpadAcceleration)
            setSensitivity(PadSettings.trackpadSensitivity)
            setPrecisionCursor(true)
            if (!PadSettings.bool(PadSettings.TP_TAP)) {
                setGestureConfig(TouchGestureConfig().copy(tapEnabled = false))
            }
        }
        trackpadPanel = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // The trackpad plus its thin scroll strip (if enabled) share one slot above the click buttons.
            addView(
                FrameLayout(context).apply {
                    layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                        val m = (10 * density).toInt()
                        setMargins(m, 0, m, m)
                    }
                    addView(trackpadView)
                    if (PadSettings.bool(PadSettings.SCROLLBAR_ON)) {
                        addView(
                            TrackpadScrollBar(context, theme) { direction -> sendWheel(direction) }.apply {
                                layoutParams = FrameLayout.LayoutParams((26 * density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                                    val edge = (4 * density).toInt()
                                    gravity = if (PadSettings.int(PadSettings.SCROLLBAR_SIDE) == 1) Gravity.START else Gravity.END
                                    if (gravity == Gravity.START) leftMargin = edge else rightMargin = edge
                                }
                            },
                        )
                    }
                },
            )
            addView(
                LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    val m = (10 * density).toInt()
                    layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (64 * density).toInt()).apply {
                        setMargins(m, 0, m, m)
                    }
                    addView(mouseButton("Left click", Pointer.Button.BUTTON_LEFT, MouseEventFlags.LEFTDOWN, MouseEventFlags.LEFTUP))
                    addView(mouseButton("Right click", Pointer.Button.BUTTON_RIGHT, MouseEventFlags.RIGHTDOWN, MouseEventFlags.RIGHTUP))
                },
            )
            visibility = View.GONE
        }
        keyboardView = ExternalOnScreenKeyboardView(context, xServer).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.BOTTOM }
            visibility = View.GONE
        }

        trackpadButton = circleButton(R.drawable.icon_trackpad, "Trackpad") { setTrackpad(trackpadPanel.visibility != View.VISIBLE) }
        keyboardButton = circleButton(R.drawable.icon_keyboard, "Keyboard") { setKeyboard(keyboardView.visibility != View.VISIBLE) }
        settingsButton = circleButton(R.drawable.icon_settings, "Settings") { setSettings(!settingsOpen) }
        settingsView = ExternalPadSettingsView(context, theme).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            // The two header buttons sit on a faint gold backing, like the button groups below.
            background = theme.groupBackground(density, strong = false)
            val side = (8 * density).toInt()
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(side, side / 2, side, 0)
            }
            val inset = (4 * density).toInt()
            setPadding(inset, inset, inset, inset)
            addView(trackpadButton)
            addView(settingsButton)
            addView(keyboardButton)
        }
        // Modifier buttons on the pad (above the number block) follow the "modifier keys shown" setting.
        // With none showing, the row and its backing stay out of the way.
        padModifierRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            val m = (3 * density).toInt()
            setPadding(0, m, 0, 0)
            if (PadSettings.bool(PadSettings.MOD_SHIFT)) addView(modifierButton("Shift", XKeycode.KEY_SHIFT_L))
            if (PadSettings.bool(PadSettings.MOD_CTRL)) addView(modifierButton("Ctrl", XKeycode.KEY_CTRL_L))
            if (PadSettings.bool(PadSettings.MOD_ALT)) addView(modifierButton("Alt", XKeycode.KEY_ALT_L))
        }
        if (padModifierRow.childCount > 0) padView.setModifierRow(padModifierRow)
        // The trackpad always has all three, above the click buttons, whatever the setting says.
        trackpadModifierRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            val m = (10 * density).toInt()
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                setMargins(m, 0, m, 0)
            }
            addView(modifierButton("Shift", XKeycode.KEY_SHIFT_L))
            addView(modifierButton("Ctrl", XKeycode.KEY_CTRL_L))
            addView(modifierButton("Alt", XKeycode.KEY_ALT_L))
        }
        trackpadPanel.addView(trackpadModifierRow, 1)
        val body = FrameLayout(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(padView)
            addView(trackpadPanel)
            addView(keyboardView)
            addView(settingsView)
        }
        addView(header)
        addView(body)
    }

    /**
     * Modifier button with three states:
     * - tap: armed (lit). It lets go by itself right after the next pad button (1-9, 0, -, =).
     * - double tap: locked (lit with a bright outline). It stays held until you tap it again.
     * - tap while armed or locked: off.
     */
    private fun modifierButton(label: String, key: XKeycode): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(theme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = theme.typeface
            contentDescription = label
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                val m = (3 * density).toInt()
                setMargins(m, 0, m, m)
            }
            fun style(active: Boolean) {
                val locked = key in lockedModifiers
                background = theme.buttonBackground(density, 12f, active, emphasized = locked, muted = true)
                setTextColor(if (active) theme.textPressed else theme.text)
            }
            style(key in heldModifiers)
            modifierStyles.getOrPut(key) { mutableListOf() }.add { active -> style(active) }
            var lastArmedAt = 0L
            setOnClickListener {
                let { PadSettings.haptic(it) }
                val now = android.os.SystemClock.uptimeMillis()
                when {
                    key !in heldModifiers -> {
                        heldModifiers.add(key)
                        xServer.injectKeyPress(key)
                        lastArmedAt = now
                        style(true)
                    }
                    key !in lockedModifiers && now - lastArmedAt <= PadSettings.doubleTapMs -> {
                        lockedModifiers.add(key)
                        style(true)
                    }
                    else -> {
                        heldModifiers.remove(key)
                        lockedModifiers.remove(key)
                        xServer.injectKeyRelease(key)
                        style(false)
                    }
                }
            }
        }
    }

    private companion object {
        /** One wheel notch, as Windows counts it (the same value the X input layer uses). */
        const val WHEEL_DELTA = 120
    }

    /** One mouse wheel tick from the scroll strip: +1 scrolls down, -1 scrolls up. Same two paths as the buttons. */
    private fun sendWheel(direction: Int) {
        if (xServer.isRelativeMouseMovement()) {
            xServer.getWinHandler().mouseEvent(MouseEventFlags.WHEEL, 0, 0, if (direction > 0) -WHEEL_DELTA else WHEEL_DELTA)
        } else {
            val button = if (direction > 0) Pointer.Button.BUTTON_SCROLL_DOWN else Pointer.Button.BUTTON_SCROLL_UP
            xServer.injectPointerButtonPress(button)
            xServer.injectPointerButtonRelease(button)
        }
    }

    /**
     * Mouse button under the trackpad. It acts like a real button: finger down presses it, finger up
     * releases it, so you can hold and drag. Double-tap and it stays pressed after you lift your finger
     * (lit with a bright outline) until you tap it again.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun mouseButton(label: String, button: Pointer.Button, downFlag: Int, upFlag: Int): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(theme.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = theme.typeface
            contentDescription = label
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                val m = (4 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            var pressed = false
            var locked = false
            var lockOnRelease = false
            var ignoreUp = false
            var lastDownAt = 0L

            fun style() {
                background = theme.buttonBackground(density, 12f, pressed || locked, emphasized = locked)
                setTextColor(if (pressed || locked) theme.textPressed else theme.text)
            }
            // Same two paths the trackpad itself uses: the Wine mouse in relative mode, X pointer buttons otherwise.
            fun send(down: Boolean) {
                if (xServer.isRelativeMouseMovement()) {
                    xServer.getWinHandler().mouseEvent(if (down) downFlag else upFlag, 0, 0, 0)
                } else if (down) {
                    xServer.injectPointerButtonPress(button)
                } else {
                    xServer.injectPointerButtonRelease(button)
                }
            }
            style()
            releaseMouseButtons.add {
                if (pressed || locked) send(false)
                pressed = false; locked = false; lockOnRelease = false; ignoreUp = false
                style()
            }
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.let { PadSettings.haptic(it) }
                        val now = SystemClock.uptimeMillis()
                        if (locked) {
                            send(false)
                            locked = false
                            pressed = false
                            ignoreUp = true
                        } else {
                            send(true)
                            pressed = true
                            lockOnRelease = now - lastDownAt <= PadSettings.doubleTapMs
                            lastDownAt = now
                        }
                        style()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        when {
                            ignoreUp -> ignoreUp = false
                            lockOnRelease && pressed -> {
                                locked = true
                                lockOnRelease = false
                            }
                            pressed -> {
                                send(false)
                                releaseModifiers()
                            }
                        }
                        pressed = false
                        style()
                    }
                }
                true
            }
        }
    }

    /** Lets go of every armed modifier. Locked ones stay down. */
    private fun releaseModifiers() {
        heldModifiers.toList().filter { it !in lockedModifiers }.forEach { key ->
            heldModifiers.remove(key)
            xServer.injectKeyRelease(key)
            modifierStyles[key]?.forEach { it(false) }
        }
    }

    /** Never leave a modifier stuck down if the display goes away. */
    override fun onDetachedFromWindow() {
        heldModifiers.toList().forEach { xServer.injectKeyRelease(it) }
        heldModifiers.clear()
        lockedModifiers.clear()
        releaseMouseButtons.forEach { it() }
        super.onDetachedFromWindow()
    }


    /** Trackpad and keyboard are mutually exclusive: opening one closes the other. */
    private fun setTrackpad(on: Boolean) {
        if (on) {
            setKeyboard(false)
            setSettings(false)
        }
        trackpadPanel.visibility = if (on) View.VISIBLE else View.GONE
        if (!on) releaseMouseButtons.forEach { it() }
        padView.visibility = if (on) View.GONE else View.VISIBLE
        // A modifier armed on one view shows as armed on the other too.
        modifierStyles.forEach { (key, styles) -> styles.forEach { it(key in heldModifiers) } }
        styleButton(trackpadButton, on)
    }

    private fun setKeyboard(on: Boolean) {
        if (on) {
            setTrackpad(false)
            setSettings(false)
        }
        keyboardView.visibility = if (on) View.VISIBLE else View.GONE
        // With the keyboard up, the space above it is left blank (just the pad background).
        padView.visibility = if (on) View.GONE else View.VISIBLE
        styleButton(keyboardButton, on)
    }

    /**
     * The settings screen replaces the pad, like the trackpad and keyboard do. Closing it rebuilds the
     * pad if any setting changed, so the new layout, look and trackpad feel apply.
     */
    private fun setSettings(on: Boolean) {
        if (on == settingsOpen) return
        if (on) {
            setTrackpad(false)
            setKeyboard(false)
            settingsVersion = PadSettings.version
            settingsOpen = true
            settingsView.visibility = View.VISIBLE
            padView.visibility = View.GONE
            styleButton(settingsButton, true)
        } else {
            settingsOpen = false
            if (PadSettings.version != settingsVersion) {
                buildUi()
            } else {
                settingsView.visibility = View.GONE
                padView.visibility = View.VISIBLE
                styleButton(settingsButton, false)
            }
        }
    }

    private fun circleButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            // The header buttons split the full width between them.
            layoutParams = LayoutParams(0, (44 * density).toInt(), 1f).apply {
                val m = (3 * density).toInt()
                setMargins(m, 0, m, 0)
            }
            setImageResource(icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            contentDescription = label
            setOnClickListener { onClick() }
            styleButton(this, false)
        }
    }

    private fun styleButton(button: ImageButton, active: Boolean) {
        button.background = theme.buttonBackground(density, 12f, active, muted = true)
        button.setColorFilter(if (active) theme.textPressed else theme.text)
    }
}
