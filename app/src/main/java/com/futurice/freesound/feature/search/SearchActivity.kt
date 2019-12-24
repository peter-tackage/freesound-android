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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.util.Log
import android.view.View
import android.widget.Toast
import com.futurice.freesound.R
import com.futurice.freesound.app.FreesoundApplication
import com.futurice.freesound.arch.mvi.view.MviBaseActivity
import com.futurice.freesound.arch.mvi.viewmodel.asUiEventFlowable
import com.futurice.freesound.common.utils.Preconditions.get
import com.futurice.freesound.common.utils.ifNull
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import com.futurice.freesound.inject.activity.BaseActivityModule
import com.jakewharton.rxbinding2.support.design.widget.dismisses
import io.reactivex.*
import kotlinx.android.synthetic.main.activity_search.*
import javax.inject.Inject

class SearchActivity : MviBaseActivity<
        SearchActivityComponent,
        SearchActivityEvent,
        SearchActivityState,
        SearchActivityViewModel
        >() {

    @Inject
    internal lateinit var searchViewModel: SearchActivityViewModel

    @Inject
    internal lateinit var schedulerProvider: SchedulerProvider

    override fun render(state: SearchActivityState) {
        Log.i("TAG", "Rendering: $state")

        state.searchTerm
                .takeIf { search_view.query !=  it }
                .also { search_view.setQuery(it, false) }

        setClearSearchVisible(state.isClearEnabled)

        state.errorMessage?.also {
            showErrorMessage(it)
        }
    }

    override fun uiEvents(): LiveData<SearchActivityEvent> {
        return LiveDataReactiveStreams.fromPublisher(searchTermChanges())
    }

    private fun searchTermChanges() = search_view.getTextChangeStream()

    private fun SearchView.getTextChangeStream() =
            Observable.create<String> { subscribeToSearchView(it) }
                    .map { SearchActivityEvent.SearchTermChanged(it) as SearchActivityEvent }
                    .asUiEventFlowable()

    private fun SearchView.subscribeToSearchView(emitter: ObservableEmitter<String>) {
        setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                emitter.onNext(get(newText))
                return true
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        savedInstanceState.ifNull { addSearchFragment() }

        toolbar_search.apply { setSupportActionBar(this) }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        search_view.apply {
            isIconified = false
            // TODO Should this only be set by the state?
            setOnCloseListener {
                setQuery(NO_SEARCH, true)
                true
            }
        }
    }

    override fun inject() {
        component().inject(this)
    }

    override fun createComponent(): SearchActivityComponent =
            (application as FreesoundApplication).component()
                    .plusSearchActivityComponent(BaseActivityModule(this),
                            SearchActivityModule(this))

    private fun addSearchFragment() {
        supportFragmentManager.beginTransaction()
                .add(R.id.container, SearchFragment.create())
                .commit()
    }

    private fun setClearSearchVisible(isClearButtonVisible: Boolean) {
        val closeButton: View = search_view.findViewById(R.id.search_close_btn)
        closeButton.visibility = if (isClearButtonVisible) View.VISIBLE else View.GONE
    }

    private fun showErrorMessage(charSequence: CharSequence) {
        Toast.makeText(this, charSequence, Toast.LENGTH_SHORT).show()
    }

    companion object {

        @JvmStatic
        fun open(context: Context) {
            Intent(context, SearchActivity::class.java)
                    .apply { context.startActivity(this) }
        }
    }
}
