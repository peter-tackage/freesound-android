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

import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import io.reactivex.Observable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * AudioPlayer implementation that uses ExoPlayer 2.
 *
 * This is not thread safe; you should only issue commands from the initialization thread.
 *
 * ExoPlayer documentation recommends that the player instance is only interacted with from
 * a single thread. Callbacks are provided on the same thread that initialized the ExoPlayer
 * instance.
 *
 * NOTE: I haven't yet found a way to determine the current playing source in ExoPlayer, so this
 * class needs to retain the URL itself. As a consequence, keep this instance with the same scope
 * as the underlying ExoPlayer instance. Otherwise you could arrive at a situation where ExoPlayer
 * is playing a source and this instance has no current URL defined.
 *
 * From what I can still see, this explanation to keep your own URI is still recommended:
 *  https://github.com/google/ExoPlayer/issues/2328
 */
internal class ExoPlayerAudioPlayer(private val exoPlayer: ExoPlayer,
                                    private val mediaSourceFactory: MediaSourceFactory,
                                    private val updatePeriod: Long,
                                    private val timeUnit: TimeUnit,
                                    private val schedulerProvider: SchedulerProvider) : AudioPlayer {

    companion object {
        private val PLAYER_PROGRESS_SCHEDULER_TAG = "PLAYER_PROGRESS_SCHEDULER"
    }

    private val playbackSourceRequestDisposable = SerialDisposable()
    private val playbackSourceRequestStream = PublishSubject.create<PlaybackSource>()
    private lateinit var currentPlaybackSource: PlaybackSource // initialized on first request

    private val exoPlayerStateOnceAndStream = ExoPlayerStateObservable(exoPlayer)
    private val exoPlayerTimePositionMsOnceAndStream = ExoPlayerProgressObservable(exoPlayer)

    override val playerStateOnceAndStream: Observable<out PlayerState>
        get() = definePlayerStateObservable()

    private fun definePlayerStateObservable(): Observable<PlayerState> {
        return exoPlayerStateOnceAndStream
                .switchMap { exoPlayerState ->
                    if (exoPlayerState.playbackState == Player.STATE_IDLE)
                        Observable.just(PlayerState.Idle)
                    else
                        streamPlayerUpdates()
                                .map { positionMs ->
                                    toPlayerState(currentPlaybackSource, exoPlayerState.toPlaybackStatus(), positionMs)
                                }

                }
    }

    private fun toPlayerState(source: PlaybackSource,
                              status: PlaybackStatus,
                              positionMs: Long
    ) = PlayerState.Assigned(source, status, positionMs)

    override fun init() {
        playbackSourceRequestDisposable
                .set(playbackSourceRequestStream
                        .switchMap { newPlaybackSource ->
                            definePlayerStateObservable()
                                    .take(1)
                                    .map { currentPlayerState -> toPlaybackRequest(newPlaybackSource, currentPlayerState) }
                                    .map { request -> Pair(request, newPlaybackSource) }
                        }
                        .subscribe({ pair -> handlePlaybackRequest(pair.first, pair.second) },
                                { e -> Timber.e(e, "Fatal error toggling playback") }))
    }

    override fun togglePlayback(playbackSource: PlaybackSource) {
        playbackSourceRequestStream.onNext(playbackSource)
    }

    override fun stopPlayback() {
        performStop()
    }

    override fun release() {
        playbackSourceRequestDisposable.dispose()
        exoPlayer.release()
    }

    private fun toPlaybackRequest(newPlaybackSource: PlaybackSource,
                                  playerState: PlayerState): PlaybackRequest {
        return when (playerState) {
            PlayerState.Idle -> PlaybackRequest.PLAY
            is PlayerState.Assigned -> if (playerState.source != newPlaybackSource || playerState.status == PlaybackStatus.ENDED || playerState.status == PlaybackStatus.ERROR) {
                PlaybackRequest.PLAY
            } else {
                if (playerState.status == PlaybackStatus.PLAYING) PlaybackRequest.PAUSE else PlaybackRequest.RESUME
            }
        }.also { Timber.i("toPlaybackRequest: state: $playerState new: $newPlaybackSource, result: $it") }
    }

    private fun handlePlaybackRequest(request: PlaybackRequest,
                                      playbackSource: PlaybackSource) {

        // Apply the change to the source
        currentPlaybackSource = playbackSource

        // Apply the change to ExoPlayer
        when (request) {
            PlaybackRequest.PLAY -> performPlay(playbackSource)
            PlaybackRequest.PAUSE, PlaybackRequest.RESUME -> performToggle(request)
        }.also { Timber.v("Request: %s, URL: %s", request, currentPlaybackSource) }
    }


    private fun performPlay(playbackSource: PlaybackSource) {
        exoPlayer.prepare(mediaSourceFactory.create(playbackSource.url))
        exoPlayer.playWhenReady = true
    }

    private fun performToggle(request: PlaybackRequest) {
        exoPlayer.playWhenReady = request != PlaybackRequest.PAUSE
    }

    private fun performStop() {
        exoPlayer.stop()
    }

    private fun ExoPlayerState.toPlaybackStatus(): PlaybackStatus {
        return when (playbackState) {
            Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            Player.STATE_READY -> if (playWhenReady)
                PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
            Player.STATE_ENDED -> PlaybackStatus.ENDED
            else -> throw IllegalStateException("Unsupported ExoPlayer status: $this")
        }
    }

    private enum class PlaybackRequest {
        PAUSE,
        RESUME,
        PLAY
    }

    private fun streamPlayerUpdates(): Observable<Long> {

        fun asUpdatingProgressOnceAndStream(updatePeriod: Long,
                                            timeUnit: TimeUnit) =
                Observable.timer(updatePeriod, timeUnit,
                        schedulerProvider.time(PLAYER_PROGRESS_SCHEDULER_TAG))
                        .observeOn(schedulerProvider.ui())
                        .repeat()
                        .startWith(0L)
                        .switchMap { exoPlayerTimePositionMsOnceAndStream }

        fun ExoPlayerState.isTimelineChanging() =
                playbackState == Player.STATE_READY && playWhenReady

        return exoPlayerStateOnceAndStream
                .switchMap { state ->
                    if (state.isTimelineChanging())
                        asUpdatingProgressOnceAndStream(updatePeriod, timeUnit)
                    else exoPlayerTimePositionMsOnceAndStream
                }
    }


}
