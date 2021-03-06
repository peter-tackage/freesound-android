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

import androidx.annotation.VisibleForTesting
import com.futurice.freesound.arch.mvvm.BaseViewModel
import com.futurice.freesound.common.Text
import com.futurice.freesound.common.rx.plusAssign
import com.futurice.freesound.feature.analytics.Analytics
import com.futurice.freesound.feature.audio.AudioPlayer
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber.e
import java.util.concurrent.TimeUnit

@VisibleForTesting
const val SEARCH_DEBOUNCE_TIME_SECONDS = 1

@VisibleForTesting
const val SEARCH_DEBOUNCE_TAG = "SEARCH DEBOUNCE"

const val NO_SEARCH = Text.EMPTY

internal class SearchActivityViewModel(private val searchRepository: SearchRepository,
                                       private val audioPlayer: AudioPlayer,
                                       private val analytics: Analytics,
                                       private val schedulerProvider: SchedulerProvider) : BaseViewModel() {

    private val searchTermOnceAndStream = BehaviorSubject.createDefault(NO_SEARCH)

    override fun bind(d: CompositeDisposable) {
        audioPlayer.init()

        d += searchTermOnceAndStream.observeOn(schedulerProvider.computation())
                .distinctUntilChanged()
                .switchMap { query ->
                    if (query.isNotEmpty())
                        querySearch(query).toObservable<Any>()
                    else
                        clearResults().toObservable<Any>()
                }
                .subscribeOn(schedulerProvider.computation())
                .subscribe({}) { e(it, "Fatal error when setting search term") }
    }

    public override fun unbind() {
        audioPlayer.release()
    }

    fun search(query: String) {
        searchTermOnceAndStream.onNext(query.trim())
    }

    val isClearEnabledOnceAndStream: Observable<Boolean>
        get() = searchTermOnceAndStream.observeOn(schedulerProvider.computation())
                .map { isCloseEnabled(it) }

    val searchStateOnceAndStream: Observable<SearchState>
        get() = searchRepository.searchStateOnceAndStream

    private fun querySearch(query: String): Completable =
            searchRepository.querySearch(query, debounceQuery())

    private fun clearResults() = searchRepository.clear()

    private fun debounceQuery(): Completable =
            Completable.timer(SEARCH_DEBOUNCE_TIME_SECONDS.toLong(),
                    TimeUnit.SECONDS,
                    schedulerProvider.time(SEARCH_DEBOUNCE_TAG))

    private fun isCloseEnabled(query: String): Boolean = query.isNotEmpty()
}
