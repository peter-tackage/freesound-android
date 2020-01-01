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

import com.futurice.freesound.arch.mvi.TransitionObserver
import com.futurice.freesound.arch.mvi.viewmodel.SimpleViewModel
import com.futurice.freesound.arch.mvi.viewmodel.asUiStateFlowable
import com.futurice.freesound.feature.audio.AudioPlayer
import com.futurice.freesound.feature.common.DisplayableItem
import com.futurice.freesound.feature.common.Navigator
import com.futurice.freesound.network.api.model.Sound

import io.reactivex.Observable

import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import com.futurice.freesound.feature.search.SearchConstants.SearchResultListItems.SOUND
import io.reactivex.FlowableTransformer

sealed class SearchFragmentEvent {
    object LoadSearchResults : SearchFragmentEvent()
    data class OpenSoundDetails(val sound: Sound) : SearchFragmentEvent()
}

data class SearchFragmentState(val inProgress: Boolean,
                               val sounds: List<DisplayableItem<Sound>>? = null)

class SearchFragmentViewModel(private val searchService: SearchService,
                              private val navigator: Navigator,
                              private val audioPlayer: AudioPlayer,
                              schedulerProvider: SchedulerProvider,
                              transitionObserver: TransitionObserver) :
        SimpleViewModel<SearchFragmentEvent, SearchFragmentState>(
                SearchFragmentState(false),
                schedulerProvider, transitionObserver, "SearchFragmentViewModel") {

    init {
        bind()
        uiEvent(SearchFragmentEvent.LoadSearchResults)
    }

    override fun transformEventToState(state: SearchFragmentState) =
            withTransforms(state, loadSounds(), openSoundDetails())

    private fun loadSounds() =
            async<SearchFragmentEvent.LoadSearchResults, SearchState, SearchFragmentState>(
                    transform = FlowableTransformer {
                        searchService.searchState.asUiStateFlowable<SearchState>()
                                .doOnNext { stopPlayback() }
                    },
                    stateUpdate = updateStateFromSearchState()
            )

    private fun updateStateFromSearchState() =
            { state: SearchFragmentState, result: SearchState ->
                when (result) {
                    SearchState.Initial -> SearchFragmentState(false)
                    is SearchState.InProgress -> state.copy(inProgress = true)
                    is SearchState.Success -> state.copy(inProgress = false,
                            sounds = wrapInDisplayableItem(result.results))
                    is SearchState.Error -> state.copy(inProgress = false)
                }
            }

    private fun openSoundDetails() =
            sync<SearchFragmentEvent.OpenSoundDetails, SearchFragmentState>(
                    stateUpdate = { state, event -> openSoundDetails(event.sound); state }
            )

    private fun stopPlayback() {
        audioPlayer.stopPlayback()
    }

    private fun openSoundDetails(sound: Sound) {
        navigator.openSoundDetails(sound)
    }

    private fun wrapInDisplayableItem(
            sounds: List<Sound>): List<DisplayableItem<Sound>> {
        return Observable.fromIterable(sounds)
                .map { sound -> DisplayableItem(sound, SOUND) }
                .toList()
                .blockingGet()
    }
}
