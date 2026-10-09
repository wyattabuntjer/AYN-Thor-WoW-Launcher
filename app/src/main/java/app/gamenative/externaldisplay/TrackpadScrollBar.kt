package app.gamenative.externaldisplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * A scroll area along one edge of the trackpad. Sliding a finger down along it scrolls down, sliding up
 * scrolls up (or the other way round with the invert setting). Touches on it never move the cursor.
 * It is only marked by a faint fixed line with a small arrow at each end; nothing on it moves.
 * [onScroll] gets +1 for a tick down and -1 for a tick up.
 */
@SuppressLint("ViewConstructor")
class TrackpadScrollBar(
    context: Context,
    private val theme: PadTheme,
    private val onScroll: (direction: Int) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private var lastY = 0f
    private var carry = 0f
    private var touching = false

    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rect = RectF()
    private val path = Path()

    private fun faint(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    override fun onDraw(canvas: Canvas) {
        // Brighter while a finger is on it, otherwise a quiet hint of where the area is.
        val lineAlpha = if (touching) 0xA0 else 0x4D
        val cx = width / 2f
        val lineW = 3 * density
        val inset = 22 * density
        line.color = faint(theme.border, lineAlpha)
        rect.set(cx - lineW / 2, inset, cx + lineW / 2, height - inset)
        canvas.drawRoundRect(rect, lineW / 2, lineW / 2, line)

        arrow.color = faint(theme.border, if (touching) 0xC0 else 0x70)
        val a = 5 * density
        path.reset()
        path.moveTo(cx, 8 * density)
        path.lineTo(cx - a, 8 * density + a * 1.4f)
        path.lineTo(cx + a, 8 * density + a * 1.4f)
        path.close()
        canvas.drawPath(path, arrow)
        path.reset()
        path.moveTo(cx, height - 8 * density)
        path.lineTo(cx - a, height - 8 * density - a * 1.4f)
        path.lineTo(cx + a, height - 8 * density - a * 1.4f)
        path.close()
        canvas.drawPath(path, arrow)
    }

    /** Finger travel per wheel tick: speed 5 is 20 dp, 1 is 100 dp, 10 is 10 dp. */
    private fun pxPerTick(): Float = 100f * density / PadSettings.int(PadSettings.SCROLLBAR_SPEED).coerceIn(1, 10)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                carry = 0f
                touching = true
                invalidate()
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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touching = false
                invalidate()
            }
        }
        return true
    }
}
