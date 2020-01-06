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

import com.futurice.freesound.network.api.FreeSoundApiService
import com.futurice.freesound.network.api.model.SoundSearchResult
import com.futurice.freesound.test.data.TestData
import com.futurice.freesound.test.rx.TrampolineSchedulerProvider

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import io.reactivex.Single

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class DefaultSearchServiceTest {

    @Mock
    private lateinit var freeSoundApiService: FreeSoundApiService

    private lateinit var defaultSearchService: DefaultSearchService

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        defaultSearchService = DefaultSearchService(freeSoundApiService,
                TrampolineSchedulerProvider())
    }

    @Test
    fun querySearch_queriesFreesoundSearchService() {
        Arrangement().withDummySearchResult()

        defaultSearchService.search(QUERY).test()

        verify<FreeSoundApiService>(freeSoundApiService).search(eq(QUERY))
    }

    @Test
    fun querySearch_completes_whenQuerySearchSuccessful() {
        Arrangement().withSearchResultsFor(QUERY, dummyResults())

        defaultSearchService.search(QUERY)
                .test()
                .assertComplete()
    }

    @Test
    fun querySearch_doesNotEmitError_whenQuerySearchErrors() {
        Arrangement().withSearchResultError(Exception())

        defaultSearchService.search("should-error")
                .test()
                .assertComplete()
    }

    @Test
    fun getSearchStateOnceAndStream_isInitiallyClear() {
        defaultSearchService.searchState
                .test()
                .assertNotTerminated()
                .assertValue(SearchState.Initialized)
    }

    @Test
    fun querySearch_triggersSearchStateProgress() {
        Arrangement().withDummySearchResult()
        val ts = defaultSearchService.searchState
                .test()

        defaultSearchService.search(QUERY).subscribe()

        ts.assertValue(SearchState.InProgress(QUERY))
    }

    @Test
    fun getSearchStateOnceAndStream_emitsResults_whenQuerySearch() {
        val expected = dummyResults()
        Arrangement().withSearchResultsFor(QUERY, expected)

        defaultSearchService.search(QUERY).subscribe()

        defaultSearchService.searchState
                .test()
                .assertNoErrors()
                .assertValue(SearchState.Success(QUERY, expected.results))
    }

    @Test
    fun getSearchStateOnceAndStream_doesNotCompleteOrError_whenQuerySearchErrors() {
        Arrangement().withSearchResultError(Exception())
        val ts = defaultSearchService.searchState
                .test()

        defaultSearchService.search("should-error").subscribe()

        ts.assertNotTerminated()
    }

    @Test
    fun getSearchStateOnceAndStream_hasNoTerminalEvent() {
        defaultSearchService.searchState
                .test()
                .assertNotTerminated()
    }

    @Test
    fun getSearchStateOnceAndStream_emitsErrorValue_whenQuerySearchErrors() {
        val searchError = Exception()
        Arrangement().withSearchResultError(searchError)
                .act()
                .querySearch()

        defaultSearchService
                .searchState.test()
                .assertNotTerminated()
                .assertValueCount(1)
                .assertValue(SearchState.Error(QUERY, searchError))
    }

    @Test
    fun getSearchStateOnceAndStream_doesNotTerminate_whenQuerySearchSuccessful() {
        Arrangement().withDummySearchResult()
                .act()
                .querySearch()

        defaultSearchService.searchState
                .test()
                .assertNotTerminated()
    }

    @Test
    fun getSearchStateOnceAndStream_doesNotTerminate_whenQuerySearchErrors() {
        Arrangement().withSearchResultError()
        val ts = defaultSearchService.searchState
                .test()

        defaultSearchService.search(QUERY).subscribe()

        ts.assertNotTerminated()
    }

    @Test
    fun getSearchStateOnceAndStream_doesNotEmitDuplicateEvents() {
        Arrangement().withDummySearchResult()
        val ts = defaultSearchService.searchState
                .skip(1) // ignore initial value
                .test()

        defaultSearchService.search(QUERY).subscribe()
        defaultSearchService.search(QUERY).subscribe()

        ts.assertValueCount(1)
    }

    @Test
    fun clear_clearsSearchState() {
        Arrangement().withDummySearchResult()
                .act()
                .querySearch()

        defaultSearchService.clear().subscribe()

        defaultSearchService.searchState
                .test()
                .assertValueCount(1)
                .assertValue(SearchState.Initialized)
                .assertNotTerminated()
    }

    @Test
    fun clear_completes() {
        defaultSearchService.clear()
                .test()
                .assertComplete()
    }

    private inner class Arrangement {

        internal fun withDummySearchResult(): Arrangement {
            `when`(freeSoundApiService.search(anyString()))
                    .thenReturn(Single.just(dummyResults()))
            return this
        }

        internal fun withSearchResultsFor(query: String, results: SoundSearchResult): Arrangement {
            `when`(freeSoundApiService.search(eq(query))).thenReturn(Single.just(results))
            return this
        }

        @JvmOverloads
        internal fun withSearchResultError(exception: Exception = Exception()): Arrangement {
            `when`(freeSoundApiService.search(any())).thenReturn(Single.error(exception))
            return this
        }

        internal fun act(): Act {
            return Act()
        }
    }

    private inner class Act {

        @JvmOverloads
        internal fun querySearch(query: String = QUERY) {
            defaultSearchService.search(query).subscribe()
        }
    }

    companion object {

        private val QUERY = "trains"

        private fun dummyResults(): SoundSearchResult {
            return TestData.searchResult(5)
        }
    }

}
