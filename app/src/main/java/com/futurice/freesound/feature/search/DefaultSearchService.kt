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

import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import com.futurice.freesound.network.api.FreeSoundApiService
import com.futurice.freesound.network.api.model.Sound
import com.futurice.freesound.network.api.model.SoundSearchResult

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

import com.futurice.freesound.common.utils.Preconditions.get

// TODO This could be decorated with a debounce.
internal class DefaultSearchService(private val freeSoundApiService: FreeSoundApiService,
                                    private val schedulerProvider: SchedulerProvider) : SearchService {

    private val searchStateStream = BehaviorSubject.createDefault<SearchState>(SearchState.Initial)

    override fun search(query: String): Completable {
        return freeSoundApiService.search(get(query))
                .doOnSubscribe { reportInProgress(query) }
                .map<List<Sound>> { toResults(it) }
                .doOnSuccess { results -> this.reportResults(query, results) }
                .doOnError { reason -> this.reportError(query, reason) }
                .toCompletable()
                .onErrorComplete() // never directly propagate errors
    }

    override fun getSearchState(): Observable<SearchState> {
        return searchStateStream
                .hide()
                .observeOn(schedulerProvider.computation())
                .distinctUntilChanged()
    }

    override fun clear(): Completable {
        return Completable.fromAction { this.reportClear() }
    }

    private fun toResults(soundSearchResult: SoundSearchResult): List<Sound> {
        return soundSearchResult.results
    }

    private fun reportClear() {
        searchStateStream.onNext(SearchState.Initial)
    }

    private fun reportInProgress(searchTerm: String) {
        searchStateStream.onNext(SearchState.InProgress(searchTerm))
    }

    private fun reportResults(searchTerm: String, results: List<Sound>) {
        searchStateStream.onNext(SearchState.Success(searchTerm, results))
    }

    private fun reportError(searchTerm: String, reason: Throwable) {
        searchStateStream.onNext(SearchState.Error(searchTerm, reason))
    }

}
