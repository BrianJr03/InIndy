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
val AddAPhotoIcon: ImageVector
    get() {
        if (_addAPhoto != null) return _addAPhoto!!
        _addAPhoto = ImageVector.Builder(
            name = "add_a_photo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
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
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 4f)
                verticalLineTo(1f)
                horizontalLineTo(5f)
                verticalLineTo(4f)
                horizontalLineTo(8f)
                verticalLineTo(6f)
                horizontalLineTo(5f)
                verticalLineTo(9f)
                horizontalLineTo(3f)
                verticalLineTo(6f)
                horizontalLineTo(0f)
                verticalLineTo(4f)
                horizontalLineTo(3f)
                close()
                moveTo(6f, 10f)
                verticalLineTo(7f)
                horizontalLineTo(9f)
                verticalLineTo(4f)
                horizontalLineTo(16f)
                lineTo(17.83f, 6f)
                horizontalLineTo(21f)
                curveTo(22.1f, 6f, 23f, 6.9f, 23f, 8f)
                verticalLineTo(20f)
                curveTo(23f, 21.1f, 22.1f, 22f, 21f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(10f)
                horizontalLineTo(6f)
                close()
                moveTo(13f, 19f)
                curveTo(15.76f, 19f, 18f, 16.76f, 18f, 14f)
                reflectiveCurveTo(15.76f, 9f, 13f, 9f)
                reflectiveCurveTo(8f, 11.24f, 8f, 14f)
                reflectiveCurveTo(10.24f, 19f, 13f, 19f)
                close()
            }
        }.build()
        return _addAPhoto!!
    }

private var _addAPhoto: ImageVector? = null
