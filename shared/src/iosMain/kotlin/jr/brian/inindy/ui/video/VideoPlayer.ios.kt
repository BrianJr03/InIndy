package jr.brian.inindy.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    autoPlay: Boolean,
    loop: Boolean
) {
    val playerAndItem = remember(url) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            val item = AVPlayerItem(uRL = nsUrl)
            AVPlayer(playerItem = item) to item
        }
    }

    DisposableEffect(playerAndItem, autoPlay, loop) {
        if (playerAndItem == null) return@DisposableEffect onDispose {}
        val (player, item) = playerAndItem

        if (autoPlay) player.play()

        val observer: Any? = if (loop) {
            NSNotificationCenter.defaultCenter.addObserverForName(
                name = AVPlayerItemDidPlayToEndTimeNotification,
                `object` = item,
                queue = null
            ) { _ ->
                player.seekToTime(kCMTimeZero.readValue())
                player.play()
            }
        } else null

        onDispose {
            player.pause()
            observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        }
    }

    UIKitViewController(
        modifier = modifier,
        factory = {
            AVPlayerViewController().apply {
                this.player = playerAndItem?.first
                showsPlaybackControls = true
            }
        }
    )
}
