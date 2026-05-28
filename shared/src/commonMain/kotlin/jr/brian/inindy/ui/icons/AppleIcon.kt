package jr.brian.inindy.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val AppleIcon: ImageVector
    get() {
        if (_appleIcon != null) return _appleIcon!!
        _appleIcon = ImageVector.Builder(
            name = "apple",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.05f, 12.04f)
                curveToRelative(-0.03f, -2.96f, 2.41f, -4.37f, 2.52f, -4.44f)
                curveToRelative(-1.37f, -2.01f, -3.51f, -2.29f, -4.27f, -2.32f)
                curveToRelative(-1.82f, -0.18f, -3.55f, 1.07f, -4.47f, 1.07f)
                curveToRelative(-0.93f, 0f, -2.34f, -1.05f, -3.85f, -1.02f)
                curveToRelative(-1.98f, 0.03f, -3.81f, 1.15f, -4.83f, 2.92f)
                curveToRelative(-2.06f, 3.57f, -0.53f, 8.85f, 1.48f, 11.74f)
                curveToRelative(0.98f, 1.42f, 2.15f, 3f, 3.69f, 2.94f)
                curveToRelative(1.49f, -0.06f, 2.05f, -0.96f, 3.85f, -0.96f)
                curveToRelative(1.79f, 0f, 2.31f, 0.96f, 3.88f, 0.93f)
                curveToRelative(1.6f, -0.03f, 2.62f, -1.44f, 3.6f, -2.86f)
                curveToRelative(1.13f, -1.64f, 1.6f, -3.24f, 1.63f, -3.32f)
                curveToRelative(-0.04f, -0.02f, -3.13f, -1.2f, -3.16f, -4.77f)
                close()
                moveTo(14.15f, 5.4f)
                curveToRelative(0.82f, -1f, 1.38f, -2.38f, 1.22f, -3.76f)
                curveToRelative(-1.18f, 0.05f, -2.62f, 0.79f, -3.47f, 1.78f)
                curveToRelative(-0.76f, 0.88f, -1.43f, 2.28f, -1.25f, 3.64f)
                curveToRelative(1.32f, 0.1f, 2.67f, -0.67f, 3.5f, -1.66f)
                close()
            }
        }.build()
        return _appleIcon!!
    }

private var _appleIcon: ImageVector? = null
