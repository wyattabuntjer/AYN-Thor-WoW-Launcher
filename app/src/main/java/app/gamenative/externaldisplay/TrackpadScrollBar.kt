package app.gamenative.externaldisplay

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View

/**
 * An invisible scroll area along one edge of the trackpad. Sliding a finger down along it scrolls down,
 * sliding up scrolls up (or the other way round with [invert]). Touches on it never move the cursor.
 * [onScroll] gets +1 for a tick down and -1 for a tick up.
 */
@SuppressLint("ViewConstructor")
class TrackpadScrollBar(
    context: Context,
    private val onScroll: (direction: Int) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private var lastY = 0f
    private var carry = 0f

    /** Finger travel per wheel tick: speed 5 is 20 dp, 1 is 100 dp, 10 is 10 dp. */
    private fun pxPerTick(): Float = 100f * density / PadSettings.int(PadSettings.SCROLLBAR_SPEED).coerceIn(1, 10)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                carry = 0f
                PadSettings.haptic(this)
            }
            MotionEvent.ACTION_MOVE -> {
                carry += event.y - lastY
                lastY = event.y
                val step = pxPerTick()
                val flip = if (PadSettings.bool(PadSettings.SCROLLBAR_INVERT)) -1 else 1
                while (carry >= step) { carry -= step; onScroll(flip) }
                while (carry <= -step) { carry += step; onScroll(-flip) }
            }
        }
        return true
    }
}
