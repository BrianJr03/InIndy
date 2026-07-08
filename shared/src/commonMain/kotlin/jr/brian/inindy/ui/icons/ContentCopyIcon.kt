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
val ContentCopyIcon: ImageVector
    get() {
        if (_contentCopy != null) return _contentCopy!!
        _contentCopy =
            ImageVector.Builder(
                name = "content_copy",
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
                        moveTo(16f, 1f)
                        lineTo(4f, 1f)
                        curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
                        verticalLineToRelative(14f)
                        horizontalLineToRelative(2f)
                        lineTo(4f, 3f)
                        horizontalLineToRelative(12f)
                        lineTo(16f, 1f)
                        close()
                        moveTo(19f, 5f)
                        lineTo(8f, 5f)
                        curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
                        verticalLineToRelative(14f)
                        curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
                        horizontalLineToRelative(11f)
                        curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
                        lineTo(21f, 7f)
                        curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                        close()
                        moveTo(19f, 21f)
                        lineTo(8f, 21f)
                        lineTo(8f, 7f)
                        horizontalLineToRelative(11f)
                        verticalLineToRelative(14f)
                        close()
                    }
                }
                .build()
        return _contentCopy!!
    }

private var _contentCopy: ImageVector? = null
