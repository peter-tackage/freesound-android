package com.futurice.freesound.feature.audio

import com.google.android.exoplayer2.Player
import io.reactivex.subjects.BehaviorSubject

class FakeObservableExoPlayer(override val stateOnceAndStream: BehaviorSubject<ExoPlayerState>
                              = BehaviorSubject.createDefault(ExoPlayerState(true, Player.STATE_IDLE)),
                              override val timePositionMsOnceAndStream: BehaviorSubject<Long>
                              = BehaviorSubject.createDefault(0)
) : ObservableExoPlayer {

    override fun play(url: String) =
            stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_READY))

    override fun stop() =
            stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_IDLE))

    override fun pause() =
            stateOnceAndStream.onNext(ExoPlayerState(false, Player.STATE_READY))

    override fun resume() =
            stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_READY))

    override fun release() {}

    fun end() {
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_ENDED))
    }

    fun buffer() {
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_BUFFERING))
    }

    fun setProgress(progress: Long) = timePositionMsOnceAndStream.onNext(progress)
}