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

import com.futurice.freesound.test.rx.TimeSkipScheduler
import com.futurice.freesound.test.rx.TrampolineSchedulerProvider
import io.reactivex.Scheduler
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExoPlayerAudioPlayerTest {

    private lateinit var schedulerProvider: TrampolineSchedulerProvider
    private lateinit var exoPlayer: FakeObservableExoPlayer
    private lateinit var exoPlayerAudioPlayer: ExoPlayerAudioPlayer

    @Before
    fun setUp() {
        schedulerProvider = TrampolineSchedulerProvider()
        exoPlayer = FakeObservableExoPlayer()
        exoPlayerAudioPlayer = ExoPlayerAudioPlayer(exoPlayer,
                100,
                TimeUnit.MILLISECONDS,
                schedulerProvider)
    }

    @Test
    fun `stop stopsExoPlayer`() {
        // given
        val testScheduler = TestScheduler()
        val playbackSource = PlaybackSource(Id("abc"), "url")
        ArrangeBuilder()
                .withTimeScheduler(testScheduler)
                .withPlayingExoPlayer(playbackSource)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.stopPlayback()
        testScheduler.triggerActions()

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Idle)
    }

    @Test
    fun `playerState is Idle when ExoPlayer is Idle`() {
        // given
        exoPlayerAudioPlayer.init()

        // when, then
        exoPlayerAudioPlayer.playerStateOnceAndStream
                .test()
                .assertValue { state -> state is PlayerState.Idle }
    }

    @Test
    fun `togglePlayback plays URL when is Idle`() {
        // given
        val testScheduler = TestScheduler()
        val playbackSource = PlaybackSource(Id("abc"), "url")
        ArrangeBuilder()
                .withTimeScheduler(testScheduler)
        exoPlayerAudioPlayer.init()
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource)

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun `togglePlayback plays URL when is ended`() {
        // given
        val testScheduler = TestScheduler()
        val playbackSource = PlaybackSource(Id("abc"), "url")
        ArrangeBuilder()
                .withTimeScheduler(testScheduler)
                .withEndedExoPlayer(playbackSource)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource)

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun `togglePlayback pauses when same URL is playing`() {
        // given
        val testScheduler = TestScheduler()
        val playbackSource = PlaybackSource(Id("abc"), "url")
        ArrangeBuilder()
                .withTimeScheduler(testScheduler)
                .withPlayingExoPlayer(playbackSource)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource)

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource,
                        PlaybackStatus.PAUSED, 0))
    }

    @Test
    fun `togglePlayback resumes URL when same URL is paused`() {
        // given
        val playbackSource = PlaybackSource(Id("abc"), "url")
        val scheduler = TestScheduler()
        ArrangeBuilder()
                .withTimeScheduler(scheduler)
                .withPausedExoPlayer(playbackSource)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource)
        scheduler.triggerActions()

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun `togglePlayback plays new URL when different URL is playing`() {
        // given
        val playbackSource1 = PlaybackSource(Id("abc"), "url1")
        val playbackSource2 = PlaybackSource(Id("def"), "url2")
        val scheduler = TestScheduler()
        ArrangeBuilder()
                .withTimeScheduler(scheduler)
                .withPlayingExoPlayer(playbackSource1)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource2)
        scheduler.triggerActions()

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource2,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun `togglePlayback plays URL different URL is paused`() {
        // given
        val playbackSource1 = PlaybackSource(Id("abc"), "url1")
        val playbackSource2 = PlaybackSource(Id("def"), "url2")
        val scheduler = TestScheduler()
        ArrangeBuilder()
                .withTimeScheduler(scheduler)
                .withPausedExoPlayer(playbackSource1)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource2)
        scheduler.triggerActions()

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource2,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun `togglePlayback plays URL different URL is ended`() {
        // given
        val playbackSource1 = PlaybackSource(Id("abc"), "url1")
        val playbackSource2 = PlaybackSource(Id("def"), "url2")
        val scheduler = TestScheduler()
        ArrangeBuilder()
                .withTimeScheduler(scheduler)
                .withEndedExoPlayer(playbackSource1)
        val states = exoPlayerAudioPlayer.playerStateOnceAndStream.test()

        // when
        exoPlayerAudioPlayer.togglePlayback(playbackSource2)
        scheduler.triggerActions()

        // then
        assertThat(states.values().last())
                .isEqualTo(PlayerState.Assigned(playbackSource2,
                        PlaybackStatus.PLAYING, 0))
    }

    @Test
    fun release_releasesExoPlayer() {
        exoPlayerAudioPlayer.release()

        assertThat(exoPlayer.isReleased()).isTrue()
    }

    private inner class ArrangeBuilder {

        init {
            withTimeSkipScheduler()
        }

        fun withPlayingExoPlayer(playbackSource: PlaybackSource): ArrangeBuilder {
            exoPlayerAudioPlayer.init()
            exoPlayerAudioPlayer.togglePlayback(playbackSource)
            return this;
        }

        fun withPausedExoPlayer(playbackSource: PlaybackSource): ArrangeBuilder {
            exoPlayerAudioPlayer.init()
            exoPlayerAudioPlayer.togglePlayback(playbackSource) // make play
            exoPlayerAudioPlayer.togglePlayback(playbackSource) // make pause
            return this;
        }

        fun withEndedExoPlayer(playbackSource: PlaybackSource): ArrangeBuilder {
            exoPlayerAudioPlayer.init()
            exoPlayerAudioPlayer.togglePlayback(playbackSource) // make play
            exoPlayer.end() // make end
            return this;
        }

        fun withProgress(progress: Long): ArrangeBuilder {
            exoPlayer.setProgress(progress)
            return this
        }

        fun withTimeScheduler(scheduler: Scheduler): ArrangeBuilder {
            schedulerProvider.setTimeScheduler({ scheduler })
            return this
        }

        fun withTimeSkipScheduler(): ArrangeBuilder {
            return withTimeScheduler(TimeSkipScheduler.instance())
        }

    }

}
