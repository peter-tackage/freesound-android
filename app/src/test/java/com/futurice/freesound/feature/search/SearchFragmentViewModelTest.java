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

package com.futurice.freesound.feature.search;

import com.futurice.freesound.arch.mvi.TransitionObserver;
import com.futurice.freesound.feature.audio.AudioPlayer;
import com.futurice.freesound.feature.common.DisplayableItem;
import com.futurice.freesound.feature.common.Navigator;
import com.futurice.freesound.network.api.model.Sound;
import com.futurice.freesound.test.data.TestData;
import com.futurice.freesound.test.rx.TrampolineSchedulerProvider;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.support.annotation.NonNull;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import static com.futurice.freesound.feature.search.SearchConstants.SearchResultListItems.SOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchFragmentViewModelTest {

    private static final String ANY_QUERY = "abc";

    private static final SearchFragmentState INITIAL_STATE = new SearchFragmentState(false, null);

    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    @Mock
    private SearchService searchService;

    @Mock
    private Navigator navigator;

    @Mock
    private AudioPlayer audioPlayer;

    @Mock
    private TransitionObserver transitionObserver;

    private SearchFragmentViewModel viewModel;

    @NotNull
    private SearchFragmentViewModel newInstance() {
        return new SearchFragmentViewModel(INITIAL_STATE, searchService, navigator, audioPlayer,
                new TrampolineSchedulerProvider(), transitionObserver);
    }

    @Test
    public void uiState_emitsInitialState_whenNoEvent() {
        // given, when
        new Arrangement();
        viewModel = newInstance();

        // then
        assertThat(viewModel.uiState()
                .getValue()).isEqualTo(INITIAL_STATE);
    }

    //
    // These test the state mapping behaviour from an initial SearchState.
    //

    @Test
    public void uiState_isInProgress_whenInitiallyInProgress() {
        // given, when
        new Arrangement().withSearchState(new SearchState.InProgress(ANY_QUERY));
        viewModel = newInstance();

        // then
        assertThat(viewModel.uiState().getValue())
                .isEqualTo(new SearchFragmentState(true, null));
    }

    @Test
    public void uiState_hasSounds_whenInitiallySuccessfulSearch() {
        // given, when
        List<Sound> sounds = TestData.sounds(10);
        new Arrangement()
                .withSearchState(new SearchState.Success(ANY_QUERY, sounds));
        viewModel = newInstance();

        // then
        assertThat(viewModel.uiState().getValue())
                .isEqualTo(new SearchFragmentState(false, expectedDisplayableItemsOf(sounds)));
    }

    @Test
    public void uiState_isNotInProgress_whenInitiallyError() {
        // given, when
        new Arrangement()
                .withSearchState(new SearchState.Error(ANY_QUERY, new Exception("msg")));
        viewModel = newInstance();

        // then
        assertThat(viewModel.uiState().getValue())
                .isEqualTo(new SearchFragmentState(false, null));
    }

    @Test
    public void stopsAudioPlayback_whenInitializing() {
        // given/when
        new Arrangement();
        viewModel = newInstance();

        // then
        verify(audioPlayer).stopPlayback();
    }

    @Test
    public void audioPlaybackStops_whenSearchResultChange() {
        // given
        Arrangement arrangement = new Arrangement();
        viewModel = newInstance();
        reset(audioPlayer); // is also invoked on initialization, so reset the mock invocation count.

        // when
        arrangement.withSearchState(new SearchState.Success(ANY_QUERY, TestData.sounds(10)));

        // then
        verify(audioPlayer).stopPlayback();
    }

    //
    // Test for interesting changes in state
    //


    // Helpers

    @NonNull
    private static List<DisplayableItem<Sound>> expectedDisplayableItemsOf(
            @NonNull final List<Sound> sounds) {
        return Observable.fromIterable(sounds)
                .map(it -> new DisplayableItem<>(it, SOUND))
                .toList()
                .blockingGet();
    }

    @SuppressWarnings("UnusedReturnValue")
    private class Arrangement {

        private final BehaviorSubject<SearchState> mockedSearchResultsStream
                = BehaviorSubject.createDefault(SearchState.Initialized.INSTANCE);

        Arrangement() {
            withSearchResultStream();
        }

        private void withSearchResultStream() {
            when(searchService.getSearchState())
                    .thenReturn(mockedSearchResultsStream);
        }

        Arrangement withSearchState(SearchState searchState) {
            mockedSearchResultsStream.onNext(searchState);
            return this;
        }

    }

}
