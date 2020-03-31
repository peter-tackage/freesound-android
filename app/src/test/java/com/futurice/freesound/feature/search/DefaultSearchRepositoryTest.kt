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

import com.futurice.freesound.network.api.FreeSoundApiClient
import com.futurice.freesound.network.api.model.SoundSearchResult
import com.futurice.freesound.test.data.TestData.Companion.searchResult
import com.futurice.freesound.test.rx.TrampolineSchedulerProvider
import io.reactivex.Completable
import io.reactivex.Single
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class DefaultSearchRepositoryTest {
    @Mock
    private lateinit var freeSoundApiClient: FreeSoundApiClient

    private lateinit var defaultSearchRepository: DefaultSearchRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        defaultSearchRepository = DefaultSearchRepository(freeSoundApiClient,
                TrampolineSchedulerProvider())
    }

    @Test
    fun `querySearch queries Freesound API`() {
        // given
        Arrangement().withDummySearchResult()

        // when
        defaultSearchRepository.querySearch(QUERY, Completable.complete()).test()

        // then
        verify(freeSoundApiClient).search(eq(QUERY))
    }

    @Test
    fun `querySearch completes whenQuerySearchSuccessful`() {
        // given
        Arrangement().withSearchResultsFor(QUERY, dummyResults())

        // when, then
        defaultSearchRepository.querySearch(QUERY, Completable.complete())
                .test()
                .assertComplete()
    }

    @Test
    fun `querySearch doesNotEmitError whenQuerySearchErrors`() {
        // given
        Arrangement().withSearchResultError(Exception())

        // when, then
        defaultSearchRepository.querySearch("should-error", Completable.complete())
                .test()
                .assertComplete()
    }

    @Test
    fun `searchStateOnceAndStream isInitiallyClear`() {
        // given, when, then
        defaultSearchRepository.searchStateOnceAndStream
                .test()
                .assertNotTerminated()
                .assertValue(SearchState.Cleared)
    }

    @Test
    fun `querySearch triggersSearchStateProgress`() {
        // given
        Arrangement().withDummySearchResult()
        val ts = defaultSearchRepository.searchStateOnceAndStream
                .skip(1)
                .test()

        // when
        defaultSearchRepository.querySearch(QUERY, Completable.complete()).subscribe()

        // then
        ts.assertValueCount(3)
        Assertions.assertThat(ts.values()[0]).isEqualTo(SearchState.InProgress(null))
        Assertions.assertThat(ts.values()[2]).isInstanceOf(SearchState.Success::class.java)
    }

    @Test
    fun `searchStateOnceAndStream emitsResults whenQuerySearch`() {
        // given
        val expected = dummyResults()
        Arrangement().withSearchResultsFor(QUERY, expected)

        // when
        defaultSearchRepository.querySearch(QUERY, Completable.complete()).subscribe()

        // then
        defaultSearchRepository.searchStateOnceAndStream
                .test()
                .assertNoErrors()
                .assertValue(SearchState.Success(expected.results))
    }

    @Test
    fun `searchStateOnceAndStream doesNotCompleteOrError whenQuerySearchErrors`() {
        // given
        Arrangement().withSearchResultError(Exception())
        val ts = defaultSearchRepository.searchStateOnceAndStream
                .test()

        // when
        defaultSearchRepository.querySearch("should-error", Completable.complete()).subscribe()

        // then
        ts.assertNotTerminated()
    }

    @Test
    fun `searchStateOnceAndStream hasNoTerminalEvent`() {
        // given, when, then
        defaultSearchRepository.searchStateOnceAndStream
                .test()
                .assertNotTerminated()
    }

    @Test
    fun `searchStateOnceAndStream emitsErrorValue whenQuerySearchErrors`() {
        // given
        val searchError = Exception()
        Arrangement().withSearchResultError(searchError)
                .act()
                .querySearch()

        // when, then
        defaultSearchRepository
                .getSearchStateOnceAndStream()
                .test()
                .assertNotTerminated()
                .assertValue(SearchState.Error(searchError))
    }

    @Test
    fun `searchStateOnceAndStream doesNotTerminate whenQuerySearchSuccessful`() {
        // given
        Arrangement().withDummySearchResult()
                .act()
                .querySearch()

        // when, then
        defaultSearchRepository.searchStateOnceAndStream
                .test()
                .assertNotTerminated()
    }

    @Test
    fun `searchStateOnceAndStream doesNotTerminate whenQuerySearchErrors`() {
        // given
        Arrangement().withSearchResultError()
        val ts = defaultSearchRepository.searchStateOnceAndStream
                .test()

        // when
        defaultSearchRepository.querySearch(QUERY, Completable.complete()).subscribe()

        // then
        ts.assertNotTerminated()
    }

    @Test
    fun `searchStateOnceAndStream doesNotEmitDuplicateEvents`() {
        // given
        Arrangement().withDummySearchResult()
        val ts = defaultSearchRepository.searchStateOnceAndStream
                .skip(1) // ignore initial value
                .test()
        // when
        defaultSearchRepository.querySearch(QUERY, Completable.never()).subscribe()
        defaultSearchRepository.querySearch(QUERY, Completable.never()).subscribe()

        // then
        ts.assertValueCount(1)
    }

    @Test
    fun `clear clearsSearchState`() {
        // given
        Arrangement().withDummySearchResult()
                .act()
                .querySearch()

        // when
        defaultSearchRepository.clear().subscribe()

        // then
        defaultSearchRepository.searchStateOnceAndStream
                .test()
                .assertNotTerminated()
                .assertValue(SearchState.Cleared)
    }

    @Test
    fun `clear completes`() {
        // given, when, then
        defaultSearchRepository.clear()
                .test()
                .assertComplete()
    }

    private inner class Arrangement {
        fun withDummySearchResult(): Arrangement {
            `when`(freeSoundApiClient.search(anyString())).thenReturn(Single.just(dummyResults()))
            return this
        }

        fun withSearchResultsFor(query: String, results: SoundSearchResult): Arrangement {
            `when`(freeSoundApiClient.search(eq(query))).thenReturn(Single.just(results))
            return this
        }

        fun withSearchResultError(exception: Exception = Exception()): Arrangement {
            `when`(freeSoundApiClient.search(any())).thenReturn(Single.error(exception))
            return this
        }

        fun act(): Act {
            return Act()
        }
    }

    private inner class Act {
        fun querySearch(query: String = QUERY) {
            defaultSearchRepository.querySearch(query, Completable.complete()).subscribe()
        }
    }

    companion object {
        private const val QUERY = "trains"
        private fun dummyResults(): SoundSearchResult = searchResult(5)
    }
}