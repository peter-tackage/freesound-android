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

package com.futurice.freesound.feature.search

import android.support.annotation.VisibleForTesting
import com.futurice.freesound.arch.mvi.TransitionObserver
import com.futurice.freesound.arch.mvi.viewmodel.SimpleViewModel
import com.futurice.freesound.arch.mvi.viewmodel.asUiStateFlowable
import com.futurice.freesound.common.Text
import com.futurice.freesound.feature.audio.AudioPlayer
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import io.reactivex.Completable
import io.reactivex.FlowableTransformer
import java.util.concurrent.TimeUnit

@VisibleForTesting
const val SEARCH_DEBOUNCE_TIME_MILLIS_SECONDS: Long = 250

@VisibleForTesting
const val SEARCH_DEBOUNCE_TAG = "SEARCH DEBOUNCE"

const val NO_SEARCH = Text.EMPTY

sealed class SearchActivityEvent {
    object LoadSearchResults : SearchActivityEvent()
    data class SearchTermChanged(val searchTerm: String) : SearchActivityEvent()
    object SearchTermCleared : SearchActivityEvent()
}

data class SearchActivityState(val searchTerm: String? = null,
                               val isClearEnabled: Boolean = false,
                               val isInProgress: Boolean = false,
                               val errorMessage: String? = null)

class SearchActivityViewModel(private val searchService: SearchService,
                              private val audioPlayer: AudioPlayer,
                              schedulerProvider: SchedulerProvider,
                              transitionObserver: TransitionObserver)
    : SimpleViewModel<SearchActivityEvent, SearchActivityState>(
        SearchActivityState(),
        schedulerProvider, transitionObserver, "SearchActivityViewModel") {

    init {
        bind()
        audioPlayer.init()
        uiEvent(SearchActivityEvent.LoadSearchResults)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    override fun transforms() = listOf(
            searchTermChangedToPerformSearch(),
            loadSearchResultsToObserveSearch(),
            searchTermClearedToClearSearch())

    private fun searchTermChangedToPerformSearch() =
            fromEventStream<SearchActivityEvent.SearchTermChanged, Unit, SearchActivityState>(
                    transform = FlowableTransformer { events ->
                        events.map { it.searchTerm }
                                .distinctUntilChanged()
                                .switchMap { searchOrClear(it) }
                    }
            )

    private fun searchOrClear(searchTerm: String) =
            (if (searchTerm.isEmpty()) clearResults()
            else performSearch(searchTerm)).map { Unit }

    private fun loadSearchResultsToObserveSearch() =
            fromEventStream<SearchActivityEvent.LoadSearchResults, SearchState, SearchActivityState>(
                    transform = FlowableTransformer {
                        it.switchMap { searchService.searchState.asUiStateFlowable() }
                    },
                    stateUpdate = toSearchActivityState())

    private fun toSearchActivityState() = { state: SearchActivityState, searchState: SearchState ->
        when (searchState) {
            SearchState.Initialized -> SearchActivityState()
            is SearchState.InProgress -> state.copy(searchTerm = searchState.searchTerm,
                    isClearEnabled = isClearEnabled(searchState.searchTerm),
                    isInProgress = true,
                    errorMessage = null)
            is SearchState.Success -> state.copy(searchTerm = searchState.searchTerm,
                    isInProgress = false,
                    isClearEnabled = isClearEnabled(searchState.searchTerm),
                    errorMessage = null)
            is SearchState.Error ->
                state.copy(searchTerm = searchState.searchTerm,
                        isInProgress = false,
                        isClearEnabled = isClearEnabled(searchState.searchTerm),
                        errorMessage = searchState.reason.message)
        }
    }

    private fun performSearch(searchTerm: String) =
            debounceQuery()
                    .andThen(search(searchTerm))
                    .toFlowable<Any>()

    private fun search(searchTerm: String) = searchService.search(searchTerm)

    private fun debounceQuery(): Completable =
            Completable.timer(SEARCH_DEBOUNCE_TIME_MILLIS_SECONDS,
                    TimeUnit.MILLISECONDS,
                    schedulerProvider.time(SEARCH_DEBOUNCE_TAG))

    private fun searchTermClearedToClearSearch() =
            fromEventStream<SearchActivityEvent.SearchTermCleared, Unit, SearchActivityState>(
                    transform = FlowableTransformer {
                        it.switchMap { clearResults() }
                    })

    private fun clearResults() = searchService.clear().toFlowable<Unit>()

    private fun isClearEnabled(searchTerm: String) = searchTerm.isNotEmpty()
}
