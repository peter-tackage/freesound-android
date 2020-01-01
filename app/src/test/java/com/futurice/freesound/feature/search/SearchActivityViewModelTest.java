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
import com.futurice.freesound.network.api.model.Sound;
import com.futurice.freesound.test.rx.TimeSkipScheduler;
import com.futurice.freesound.test.rx.TrampolineSchedulerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.BehaviorSubject;

import static com.futurice.freesound.feature.search.SearchActivityViewModelKt.SEARCH_DEBOUNCE_TAG;
import static com.futurice.freesound.feature.search.SearchActivityViewModelKt.SEARCH_DEBOUNCE_TIME_MILLIS_SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchActivityViewModelTest {

    private static final String DUMMY_QUERY = "test-query";

    @Mock
    private SearchService searchService;

    @Mock
    private AudioPlayer audioPlayer;

    @Mock
    private TransitionObserver transitionObserver;

    private TrampolineSchedulerProvider schedulerProvider;

    private SearchActivityViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        schedulerProvider = new TrampolineSchedulerProvider();
        viewModel = new SearchActivityViewModel(searchService,
                audioPlayer,
                schedulerProvider,
                transitionObserver);
    }

    @Test
    public void audioPlayer_isInitialized() {
        // given, when, then
        verify(audioPlayer).init();
    }

    @Test
    public void onCleared_releasesAudioPlayer() {
        // given, when
        viewModel.onCleared();

        // then
        verify(audioPlayer).release();
    }

    @Test
    public void clear_isDisabled_afterInitialized() {
        new ArrangeBuilder();

        assertThat(viewModel.uiState()
                .getValue().isClearEnabled()).isFalse();
    }

    @Test
    public void search_queriesSearchDataModelWithTerm() {
        new ArrangeBuilder().withSuccessfulSearchResultStream();

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));

        verify(searchService).search(eq(DUMMY_QUERY));
    }

    @Test
    public void search_duplicateNonEmptyQueriesAreIgnored() {
        new ArrangeBuilder().withSuccessfulSearchResultStream();

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));

        verify(searchService).search(eq(DUMMY_QUERY));
    }

    @Test
    public void search_duplicateEmptyQueriesAreIgnored() {
        new ArrangeBuilder();

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));
        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));

        verify(searchService).clear();
    }

    @Test
    public void clear_isEnabled_whenSearchWithNonEmptyQuery() {
        new ArrangeBuilder().withSuccessfulSearchResultStream();

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));

        assertThat(viewModel.uiState()
                .getValue().isClearEnabled()).isTrue();
    }

    @Test
    public void clear_isDisableEnabled_whenSearchWithNonEmptyQuery() {
        new ArrangeBuilder().withSuccessfulSearchResultStream();

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));

        assertThat(viewModel.uiState()
                .getValue().isClearEnabled()).isFalse();
    }

    @Test
    public void search_withEmptyQuery_clearsSearchImmediately_afterNonEmptySearch() {
        TestScheduler testScheduler = new TestScheduler();
        new ArrangeBuilder()
                .withTimeScheduler(testScheduler);
                viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));

        testScheduler.advanceTimeBy(SEARCH_DEBOUNCE_TIME_MILLIS_SECONDS,
                TimeUnit.SECONDS);

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(""));

        verify(searchService, times(2)).clear();
    }

    @Test
    public void search_withNonEmptyQuery_isNotSearchedBeforeDebounce() {
        TestScheduler testScheduler = new TestScheduler();
        new ArrangeBuilder()
                .withTimeScheduler(testScheduler);

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));

        verify(searchService, never()).search(eq(DUMMY_QUERY));
    }

    @Test
    public void search_withNonEmptyQuery_isSearchedAfterDebounce() {
        TestScheduler testScheduler = new TestScheduler();
        new ArrangeBuilder()
                .withTimeScheduler(testScheduler, SEARCH_DEBOUNCE_TAG);

        viewModel.uiEvent(new SearchActivityEvent.SearchTermChanged(DUMMY_QUERY));
        testScheduler.advanceTimeBy(SEARCH_DEBOUNCE_TIME_MILLIS_SECONDS,
                TimeUnit.MILLISECONDS);

        verify(searchService).search(eq(DUMMY_QUERY));
    }

    private class ArrangeBuilder {

        private final BehaviorSubject<SearchState> searchResultsStream = BehaviorSubject
                .createDefault(SearchState.Initial.INSTANCE);

        ArrangeBuilder() {
            Mockito.when(searchService.getSearchState()).thenReturn(searchResultsStream);
            Mockito.when(searchService.clear()).thenReturn(Completable.complete());
            Mockito.when(searchService.search(anyString())).thenReturn(Completable.complete());
            withSuccessfulSearchResultStream();
            withTimeSkipScheduler();
        }

        ArrangeBuilder withTimeScheduler(Scheduler scheduler, String tag) {
            schedulerProvider.setTimeScheduler(s -> s.endsWith(tag) ? scheduler : null);
            return this;
        }

        ArrangeBuilder withTimeScheduler(Scheduler scheduler) {
            schedulerProvider.setTimeScheduler(__ -> scheduler);
            return this;
        }

        ArrangeBuilder withTimeSkipScheduler() {
            return withTimeScheduler(TimeSkipScheduler.instance());
        }

        ArrangeBuilder withSuccessfulSearchResultStream() {
            when(searchService.getSearchState())
                    .thenReturn(searchResultsStream);
            return this;
        }

        ArrangeBuilder enqueueSearchResults(String searchTerm, List<Sound> sounds) {
            searchResultsStream.onNext(new SearchState.Success(searchTerm, sounds));
            return this;
        }

        ArrangeBuilder withErrorWhenSearching() {
            when(searchService.search(anyString())).thenReturn(
                    Completable.error(new Exception()));
            return this;
        }

    }

}
