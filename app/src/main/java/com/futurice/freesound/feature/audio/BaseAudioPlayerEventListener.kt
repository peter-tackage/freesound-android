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

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

import java.util.concurrent.atomic.AtomicBoolean

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

import com.futurice.freesound.common.utils.Preconditions.get

/**
 * Base class for making Observables from ExoPlayer callback events.
 *
 * @param <T> the Observable value type.
</T> */
internal abstract class BaseAudioPlayerEventListener<T>(protected val exoPlayer: ExoPlayer,
                                                        protected val observer: Observer<in T>) : Disposable, Player.EventListener {

    private val unsubscribed = AtomicBoolean()

    override fun onLoadingChanged(isLoading: Boolean) {
        // Override if needed
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        // Override if needed
    }

    override fun onPositionDiscontinuity(reason: Int) {
        // Override if needed
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        // Override if needed
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        // Override if needed
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?,
                                 trackSelections: TrackSelectionArray?) {
        // Override if needed
    }

    override fun dispose() {
        if (unsubscribed.compareAndSet(false, true)) {
            exoPlayer.removeListener(this)
        }
    }

    override fun isDisposed(): Boolean {
        return unsubscribed.get()
    }
}
