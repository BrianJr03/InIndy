package jr.brian.inindy.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val NotificationsIcon: ImageVector
    get() {
        if (_notifications != null) {
            return _notifications!!
        }
        _notifications =
            ImageVector.Builder(
                name = "notifications",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(4f, 19f)
                        verticalLineTo(17f)
                        horizontalLineTo(6f)
                        verticalLineTo(10f)
                        quadTo(6f, 7.93f, 7.25f, 6.31f)
                        reflectiveQuadTo(10.5f, 4.2f)
                        verticalLineTo(3.5f)
                        quadToRelative(0f, -0.63f, 0.44f, -1.06f)
                        quadTo(11.38f, 2f, 12f, 2f)
                        reflectiveQuadToRelative(1.06f, 0.44f)
                        quadToRelative(0.44f, 0.43f, 0.44f, 1.06f)
                        verticalLineToRelative(0.7f)
                        quadToRelative(2f, 0.5f, 3.25f, 2.11f)
                        reflectiveQuadTo(18f, 10f)
                        verticalLineToRelative(7f)
                        horizontalLineToRelative(2f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(4f)
                        close()
                        moveToRelative(8f, 3f)
                        quadToRelative(-0.83f, 0f, -1.41f, -0.59f)
                        quadTo(10f, 20.83f, 10f, 20f)
                        horizontalLineToRelative(4f)
                        quadToRelative(0f, 0.83f, -0.59f, 1.41f)
                        quadTo(12.83f, 22f, 12f, 22f)
                        close()
                        moveTo(8f, 17f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(-7f)
                        quadToRelative(0f, -1.65f, -1.17f, -2.83f)
                        quadTo(13.65f, 6f, 12f, 6f)
                        reflectiveQuadTo(9.17f, 7.17f)
                        quadTo(8f, 8.35f, 8f, 10f)
                        close()
                    }
                }
                .build()
        return _notifications!!
    }

private var _notifications: ImageVector? = null
