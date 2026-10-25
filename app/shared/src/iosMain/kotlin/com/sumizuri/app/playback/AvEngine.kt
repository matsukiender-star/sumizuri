@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.playback

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*

private const val PollMs = 250L

// Plays a video with AVPlayer of the system. It plays what the system can play, which is the streams and files of the usual kinds. The tracks of subtitles and sound that come in the video, and the ones added from files, are not offered yet, so the lists stay empty and choosing one does nothing.
class AvEngine : PlayerEngine {
    private val mutable = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = mutable

    val player = AVPlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var speed = 1f

    init {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
        scope.launch {
            while (isActive) {
                refresh()
                delay(PollMs)
            }
        }
    }

    override fun open(media: PlayerMedia, startMs: Long, play: Boolean) {
        val address = NSURL.URLWithString(media.url)
        if (address == null) {
            mutable.update {
                it.copy(
                    problem = PlayerProblem.Failed,
                    message = "The address of the video is not valid",
                )
            }
            return
        }
        // The headers a site wants with its video go in an option the system reads, though it is not documented.
        val asset = AVURLAsset(uRL = address, options = mapOf("AVURLAssetHTTPHeaderFieldsKey" to media.headers))
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset = asset))
        mutable.update { PlayerState(speed = speed, volume = it.volume) }
        if (startMs > 0) seekTo(startMs)
        if (play) play()
    }

    override fun play() {
        player.playImmediatelyAtRate(speed)
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, 600))
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (player.rate > 0f) player.rate = speed
        mutable.update { it.copy(speed = speed) }
    }

    override fun setVolume(percent: Int) {
        player.volume = (percent / 100f).coerceIn(0f, 1f)
        mutable.update { it.copy(volume = percent) }
    }

    override fun selectSubtitle(id: String?) = Unit

    override fun selectAudio(id: String) = Unit

    override fun release() {
        scope.cancel()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    // Reads how the player is doing and puts it in the state.
    private fun refresh() {
        val item = player.currentItem ?: return
        val seconds = CMTimeGetSeconds(player.currentTime())
        val total = CMTimeGetSeconds(item.duration)
        val position = if (seconds.isFinite()) (seconds * 1000).toLong() else 0L
        val length = if (total.isFinite()) (total * 1000).toLong() else 0L
        val size = item.presentationSize.useContents { width.toInt() to height.toInt() }
        val failed = item.status == AVPlayerItemStatusFailed
        mutable.update {
            it.copy(
                ready = length > 0,
                playing = player.rate > 0f,
                buffering = player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate,
                ended = length > 0 && position >= length - 500,
                positionMs = position,
                durationMs = length,
                videoWidth = size.first,
                videoHeight = size.second,
                problem = if (failed) PlayerProblem.Failed else null,
                message = if (failed) item.error?.localizedDescription else null,
            )
        }
    }
}
