/*
 * Copyright 2016 Futurice GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futurice.freesound.feature.audio

import androidx.core.util.Pair
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import io.reactivex.Observable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.PublishSubject
import polanski.option.AtomicOption
import polanski.option.Option
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * AudioPlayer implementation that uses ExoPlayer 2.
 *
 *
 * This is not thread safe; you should only issue commands from the initialization thread.
 *
 *
 * ExoPlayer documentation recommends that the player instance is only interacted with from
 * a single thread. Callbacks are provided on the same thread that initialized the ExoPlayer
 * instance.
 *
 *
 * NOTE: I haven't yet found a way to determine the current playing source in ExoPlayer, so this
 * class needs to retain the URL itself. As a consequence, keep this instance with the same scope
 * as the underlying ExoPlayer instance. Otherwise you could arrive at a situation where ExoPlayer
 * is playing a source and this instance has no current URL defined.
 */
internal class ExoPlayerAudioPlayer(private val exoPlayer: ExoPlayer,
                                    private val observableExoPlayer: ObservableExoPlayer,
                                    private val mediaSourceFactory: MediaSourceFactory) : AudioPlayer {

    companion object {
        private val DEFAULT_UPDATE_PERIOD_MILLIS = 50
    }

    private val playerStateDisposable = SerialDisposable()
    private val toggleSourceStream = PublishSubject.create<PlaybackSource>()
    private val currentSource = AtomicOption<PlaybackSource>()

    override val playerStateOnceAndStream: Observable<PlayerState>
        get() = observableExoPlayer.getExoPlayerStateOnceAndStream()
                .map { it.toState() }
                .map { state -> PlayerState(state, currentSource.get()) }

    override val timePositionMsOnceAndStream: Observable<Long>
        get() = observableExoPlayer.getTimePositionMsOnceAndStream(DEFAULT_UPDATE_PERIOD_MILLIS.toLong(),
                TimeUnit.MILLISECONDS)

    private enum class ToggleAction {
        PAUSE,
        UNPAUSE,
        PLAY
    }

    override fun init() {
        playerStateDisposable
                .set(toggleSourceStream.concatMap { playbackSource ->
                    observableExoPlayer
                            .getExoPlayerStateOnceAndStream()
                            .take(1)
                            .map { exoPlayerState -> toToggleAction(playbackSource, exoPlayerState) }
                            .map { action -> Pair.create(action, playbackSource) }
                }
                        .subscribe({ pair -> handleToggleAction(pair.first!!, pair.second!!) },
                                { e -> Timber.e(e, "Fatal error toggling playback") }))
    }

    override fun togglePlayback(playbackSource: PlaybackSource) {
        toggleSourceStream.onNext(playbackSource)
    }

    override fun stopPlayback() {
        stop()
    }

    override fun release() {
        playerStateDisposable.dispose()
        exoPlayer.release()
    }

    private fun toToggleAction(playbackSource: PlaybackSource,
                               exoPlayerState: ExoPlayerState): ToggleAction {
        return if (exoPlayerState.isIdle() || playbackSource.hasSourceChanged()) {
            ToggleAction.PLAY
        } else {
            if (exoPlayerState.playWhenReady) ToggleAction.PAUSE else ToggleAction.UNPAUSE
        }
    }

    private fun handleToggleAction(action: ToggleAction,
                                   playbackSource: PlaybackSource) =
            when (action) {
                ToggleAction.PLAY -> play(playbackSource)
                ToggleAction.PAUSE, ToggleAction.UNPAUSE -> toggle(action)
            }.also { Timber.v("Action: %s, URL: %s", action, playbackSource) }

    private fun play(playbackSource: PlaybackSource) {
        exoPlayer.prepare(mediaSourceFactory.create(playbackSource.url))
        exoPlayer.playWhenReady = true
        currentSource.set(Option.ofObj(playbackSource))
    }

    private fun toggle(action: ToggleAction) {
        exoPlayer.playWhenReady = action != ToggleAction.PAUSE
    }

    private fun stop() {
        currentSource.set(Option.none())
        exoPlayer.stop()
    }

    private fun PlaybackSource.hasSourceChanged(): Boolean {
        return currentSource.get()
                .map { playbackSource -> playbackSource != this }
                .orDefault { false }
    }

    private fun ExoPlayerState.isIdle(): Boolean =
            this.playbackState == Player.STATE_IDLE || this.playbackState == Player.STATE_ENDED

    private fun ExoPlayerState.toState(): State {
        when (playbackState) {
            Player.STATE_IDLE -> return State.IDLE
            Player.STATE_BUFFERING -> return State.BUFFERING
            Player.STATE_READY -> return if (playWhenReady)
                State.PLAYING
            else
                State.PAUSED
            Player.STATE_ENDED -> return State.ENDED
            else -> throw IllegalStateException("Unsupported ExoPlayer state: $this")
        }
    }

}
