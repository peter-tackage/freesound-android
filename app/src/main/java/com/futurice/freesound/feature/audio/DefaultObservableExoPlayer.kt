/*
 * Copyright 2017 Futurice GmbH
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
import com.google.android.exoplayer2.Player
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

internal class DefaultObservableExoPlayer(private val stateOnceAndStream: Observable<ExoPlayerState>,
                                          private val progressOnceAndStream: Observable<Long>,
                                          private val schedulerProvider: SchedulerProvider) : ObservableExoPlayer {
    companion object {
        private val PLAYER_PROGRESS_SCHEDULER_TAG = "PLAYER_PROGRESS_SCHEDULER"
    }

    override fun getExoPlayerStateOnceAndStream(): Observable<ExoPlayerState> = stateOnceAndStream

    override fun getTimePositionMsOnceAndStream(updatePeriod: Long,
                                                timeUnit: TimeUnit) =
            stateOnceAndStream
                    .map { isTimelineChanging(it) }
                    .switchMap { isTimelineChanging ->
                        timePositionMsOnceAndStream(isTimelineChanging,
                                updatePeriod,
                                timeUnit)
                    }

    private fun timePositionMsOnceAndStream(isTimelineChanging: Boolean,
                                            updatePeriod: Long,
                                            timeUnit: TimeUnit) = if (isTimelineChanging)
        updatingProgressOnceAndStream(updatePeriod, timeUnit)
    else
        progressOnceAndStream

    private fun updatingProgressOnceAndStream(updatePeriod: Long,
                                              timeUnit: TimeUnit) =
            Observable.timer(updatePeriod, timeUnit,
                    schedulerProvider.time(PLAYER_PROGRESS_SCHEDULER_TAG))
                    .observeOn(schedulerProvider.ui())
                    .repeat()
                    .startWith(0L)
                    .switchMap { progressOnceAndStream }

    private fun isTimelineChanging(playerState: ExoPlayerState): Boolean {
        return playerState.playbackState == Player.STATE_READY && playerState.playWhenReady
    }
}
