package com.futurice.freesound.feature.audio

import com.google.android.exoplayer2.Player
import io.reactivex.subjects.BehaviorSubject

class FakeObservableExoPlayer(override val stateOnceAndStream: BehaviorSubject<ExoPlayerState>
                              = BehaviorSubject.createDefault(ExoPlayerState(true, Player.STATE_IDLE)),
                              override val timePositionMsOnceAndStream: BehaviorSubject<Long>
                              = BehaviorSubject.createDefault(0)
) : ObservableExoPlayer {

    private var isReleased: Boolean = false;

    override fun play(url: String) {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_READY))
    }

    private fun checkIsNotReleased() {
        require(!isReleased, { "Player has already been released" })
    }

    override fun stop() {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_IDLE))
    }

    override fun pause() {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(false, Player.STATE_READY))
    }

    override fun resume() {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_READY))
    }

    override fun release() {
        isReleased = true
    }

    fun end() {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_ENDED))
    }

    fun buffer() {
        checkIsNotReleased()
        stateOnceAndStream.onNext(ExoPlayerState(true, Player.STATE_BUFFERING))
    }

    fun setProgress(progress: Long) {
        checkIsNotReleased()
        timePositionMsOnceAndStream.onNext(progress)
    }

    fun isReleased() = isReleased;
}