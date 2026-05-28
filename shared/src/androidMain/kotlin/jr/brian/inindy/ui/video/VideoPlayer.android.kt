package jr.brian.inindy.ui.video

import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean
) {
    val videoView = remember(url) { VideoViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val view = VideoView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
                setVideoURI(Uri.parse(url))
                setMediaController(MediaController(context).apply { setAnchorView(container) })
                setOnPreparedListener { player ->
                    player.isLooping = loop
                    if (autoPlay) start()
                }
            }
            videoView.view = view
            container.addView(view)
            container
        }
    )

    DisposableEffect(url) {
        onDispose {
            videoView.view?.stopPlayback()
            videoView.view = null
        }
    }
}

private class VideoViewHolder(var view: VideoView? = null)
