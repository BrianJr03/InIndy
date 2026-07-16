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
val ChatIcon: ImageVector
    get() {
        if (_chat != null) return _chat!!
        _chat = ImageVector.Builder(
            name = "chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(2f, 22f)
                verticalLineTo(4f)
                quadToRelative(0f, -0.82f, 0.59f, -1.41f)
                quadTo(3.18f, 2f, 4f, 2f)
                horizontalLineToRelative(16f)
                quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                quadTo(22f, 3.18f, 22f, 4f)
                verticalLineToRelative(12f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                quadToRelative(-0.59f, 0.59f, -1.41f, 0.59f)
                horizontalLineTo(6f)
                close()
                moveTo(5.15f, 16f)
                horizontalLineTo(20f)
                verticalLineTo(4f)
                horizontalLineTo(4f)
                verticalLineToRelative(13.13f)
                close()
                moveTo(4f, 16f)
                verticalLineTo(4f)
                close()
            }
        }.build()
        return _chat!!
    }

private var _chat: ImageVector? = null
