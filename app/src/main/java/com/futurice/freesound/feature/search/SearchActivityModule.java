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

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.futurice.freesound.arch.mvi.TransitionObserver;
import com.futurice.freesound.arch.mvi.view.Binder;
import com.futurice.freesound.feature.audio.AudioModule;
import com.futurice.freesound.feature.audio.AudioPlayer;
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider;
import com.futurice.freesound.feature.home.user.HomeFragmentViewModel;
import com.futurice.freesound.inject.activity.ActivityScope;
import com.futurice.freesound.inject.activity.BaseActivityModule;
import com.futurice.freesound.network.api.FreeSoundApiService;

import dagger.Module;
import dagger.Provides;

@Module(includes = {BaseActivityModule.class, AudioModule.class})
public class SearchActivityModule {

    private final SearchActivity searchActivity;

    SearchActivityModule(SearchActivity searchActivity) {
        this.searchActivity = searchActivity;
    }

    @Provides
     SearchActivityViewModel provideSearchViewModel(
            SearchService searchService,
            AudioPlayer audioPlayer,
            SchedulerProvider schedulerProvider,
            TransitionObserver transitionObserver) {

        return ViewModelProviders.of(searchActivity, new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull final Class<T> modelClass) {
                return (T) new SearchActivityViewModel(searchService,
                        audioPlayer,
                        schedulerProvider,
                        transitionObserver);
            }
        }).get(SearchActivityViewModel.class);

    }


    @Provides
    @ActivityScope
    static SearchService provideSearchDataModel(FreeSoundApiService freeSoundApiService,
                                                SchedulerProvider schedulerProvider) {
        return new DefaultSearchService(freeSoundApiService, schedulerProvider);
    }

    @Provides
    @ActivityScope
    Binder<SearchActivityEvent, SearchActivityState, SearchActivityViewModel> provideBinder(
            SearchActivityViewModel viewModel) {
        return new Binder<>(searchActivity, viewModel, searchActivity);
    }

}
