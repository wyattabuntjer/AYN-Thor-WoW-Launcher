package app.gamenative.externaldisplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * A thin scroll strip along one edge of the trackpad. Dragging a finger down or up along it sends mouse
 * wheel ticks (down / up); [onScroll] gets +1 for a tick down and -1 for a tick up. The strip drawn is a
 * few pixels wide, but the touch area is wider so a thumb can find it, and touches on it never move the
 * cursor. A small thumb follows the finger for feedback.
 */
@SuppressLint("ViewConstructor")
class TrackpadScrollBar(
    context: Context,
    private val theme: PadTheme,
    private val onScroll: (direction: Int) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var lastY = 0f
    private var carry = 0f
    private var thumbFraction = 0.5f
    private var dragging = false

    /** Finger travel per wheel tick: speed 5 is 20 dp, 1 is 100 dp, 10 is 10 dp. */
    private fun pxPerTick(): Float = 100f * density / PadSettings.int(PadSettings.SCROLLBAR_SPEED).coerceIn(1, 10)

    override fun onDraw(canvas: Canvas) {
        val barW = 6 * density
        val cx = width / 2f
        val pad = 8 * density
        rect.set(cx - barW / 2, pad, cx + barW / 2, height - pad)
        track.color = (theme.keyBottom and 0x00FFFFFF) or (0xCC shl 24)
        trackEdge.color = theme.border
        trackEdge.strokeWidth = density.coerceAtLeast(1f)
        canvas.drawRoundRect(rect, barW / 2, barW / 2, track)
        canvas.drawRoundRect(rect, barW / 2, barW / 2, trackEdge)

        val thumbH = 28 * density
        val travel = (rect.height() - thumbH).coerceAtLeast(0f)
        val top = rect.top + travel * thumbFraction
        thumb.color = if (dragging) theme.borderBright else theme.border
        canvas.drawRoundRect(cx - barW / 2, top, cx + barW / 2, top + thumbH, barW / 2, barW / 2, thumb)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = event.y
                carry = 0f
                dragging = true
                PadSettings.haptic(this)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastY
                lastY = event.y
                carry += dy
                val step = pxPerTick()
                while (carry >= step) { carry -= step; onScroll(+1) }
                while (carry <= -step) { carry += step; onScroll(-1) }
                // The thumb follows the finger and wraps around, so it keeps moving on a long scroll.
                thumbFraction = (thumbFraction + dy / height).let { f -> f - Math.floor(f.toDouble()).toFloat() }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
            }
        }
        return true
    }
}
