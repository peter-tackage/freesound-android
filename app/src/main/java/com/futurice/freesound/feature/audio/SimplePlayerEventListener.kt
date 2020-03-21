package com.futurice.freesound.feature.audio

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

internal abstract class SimplePlayerEventListener : Player.EventListener {

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        // Override if needed
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        // Override if needed

    }

    override fun onLoadingChanged(isLoading: Boolean) {
        // Override if needed
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        // Override if needed
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        // Override if needed
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        // Override if needed
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        // Override if needed
    }

    override fun onPositionDiscontinuity(reason: Int) {
        // Override if needed
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        // Override if needed
    }

    override fun onSeekProcessed() {
        // Override if needed
    }
}
