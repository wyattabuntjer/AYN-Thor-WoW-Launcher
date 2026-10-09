package app.gamenative.ui.screen.wow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A key with a diagonal line through it: the Material "key" icon plus a slash. */
val KeyOffIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "KeyOff",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // The key.
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.65f, 10f)
            curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
            curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
            reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
            curveToRelative(2.61f, 0f, 4.83f, -1.67f, 5.65f, -4f)
            horizontalLineTo(17f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-4f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(-4f)
            horizontalLineTo(12.65f)
            close()
            moveTo(7f, 14f)
            curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
            reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
            reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
            reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
            close()
        }
        // The line through it.
        path(fill = SolidColor(Color.Black)) {
            moveTo(2.4f, 4.4f)
            lineTo(4.4f, 2.4f)
            lineTo(22.6f, 20.6f)
            lineTo(20.6f, 22.6f)
            close()
        }
    }.build()
}
