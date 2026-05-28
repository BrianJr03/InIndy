package jr.brian.inindy.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val GoogleIcon: ImageVector
    get() {
        if (_googleIcon != null) return _googleIcon!!
        _googleIcon = ImageVector.Builder(
            name = "google",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).apply {
            path(fill = SolidColor(Color(0xFFFFC107))) {
                moveTo(43.611f, 20.083f)
                horizontalLineTo(42f)
                verticalLineTo(20f)
                horizontalLineTo(24f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(11.303f)
                curveToRelative(-1.649f, 4.657f, -6.08f, 8f, -11.303f, 8f)
                curveToRelative(-6.627f, 0f, -12f, -5.373f, -12f, -12f)
                reflectiveCurveToRelative(5.373f, -12f, 12f, -12f)
                curveToRelative(3.059f, 0f, 5.842f, 1.154f, 7.961f, 3.039f)
                lineToRelative(5.657f, -5.657f)
                curveTo(34.046f, 6.053f, 29.268f, 4f, 24f, 4f)
                curveTo(12.955f, 4f, 4f, 12.955f, 4f, 24f)
                reflectiveCurveToRelative(8.955f, 20f, 20f, 20f)
                reflectiveCurveToRelative(20f, -8.955f, 20f, -20f)
                curveTo(44f, 22.659f, 43.862f, 21.35f, 43.611f, 20.083f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFF3D00))) {
                moveTo(6.306f, 14.691f)
                lineToRelative(6.571f, 4.819f)
                curveTo(14.655f, 15.108f, 18.961f, 12f, 24f, 12f)
                curveToRelative(3.059f, 0f, 5.842f, 1.154f, 7.961f, 3.039f)
                lineToRelative(5.657f, -5.657f)
                curveTo(34.046f, 6.053f, 29.268f, 4f, 24f, 4f)
                curveTo(16.318f, 4f, 9.656f, 8.337f, 6.306f, 14.691f)
                close()
            }
            path(fill = SolidColor(Color(0xFF4CAF50))) {
                moveTo(24f, 44f)
                curveToRelative(5.166f, 0f, 9.86f, -1.977f, 13.409f, -5.192f)
                lineToRelative(-6.19f, -5.238f)
                curveTo(29.211f, 35.091f, 26.715f, 36f, 24f, 36f)
                curveToRelative(-5.202f, 0f, -9.619f, -3.317f, -11.283f, -7.946f)
                lineToRelative(-6.522f, 5.025f)
                curveTo(9.505f, 39.556f, 16.227f, 44f, 24f, 44f)
                close()
            }
            path(fill = SolidColor(Color(0xFF1976D2))) {
                moveTo(43.611f, 20.083f)
                horizontalLineTo(42f)
                verticalLineTo(20f)
                horizontalLineTo(24f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(11.303f)
                curveToRelative(-0.792f, 2.237f, -2.231f, 4.166f, -4.087f, 5.571f)
                curveToRelative(0.001f, -0.001f, 0.002f, -0.001f, 0.003f, -0.002f)
                lineToRelative(6.19f, 5.238f)
                curveTo(36.971f, 39.205f, 44f, 34f, 44f, 24f)
                curveTo(44f, 22.659f, 43.862f, 21.35f, 43.611f, 20.083f)
                close()
            }
        }.build()
        return _googleIcon!!
    }

private var _googleIcon: ImageVector? = null
