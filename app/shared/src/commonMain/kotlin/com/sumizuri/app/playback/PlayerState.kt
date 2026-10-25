package com.sumizuri.app.playback

// Why a video cannot play: the player of the system is not there, or the video failed.
enum class PlayerProblem { EngineMissing, Failed }

// How the engine is doing. The times are in milliseconds. The subtitles and the audios are the tracks the video has, with the ones that were added from files, and the ones chosen are named by id.
data class PlayerState(
    val ready: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = true,
    val ended: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val speed: Float = 1f,
    val volume: Int = 100,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val subtitles: List<PlayerTrack> = emptyList(),
    val audios: List<PlayerTrack> = emptyList(),
    val subtitle: String? = null,
    val audio: String? = null,
    val problem: PlayerProblem? = null,
    val message: String? = null,
)
