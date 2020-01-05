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
import com.futurice.freesound.arch.mvi.view.Binder;
import com.futurice.freesound.feature.audio.AudioPlayer;
import com.futurice.freesound.feature.common.Navigator;
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider;
import com.futurice.freesound.feature.common.ui.adapter.ItemComparator;
import com.futurice.freesound.feature.common.ui.adapter.RecyclerViewAdapter;
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderBinder;
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderFactory;
import com.futurice.freesound.inject.activity.ActivityScope;
import com.futurice.freesound.inject.activity.ForActivity;
import com.futurice.freesound.inject.fragment.BaseFragmentModule;
import com.futurice.freesound.inject.fragment.FragmentScope;
import com.futurice.freesound.network.api.model.Sound;
import com.squareup.picasso.Picasso;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Map;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntKey;
import dagger.multibindings.IntoMap;

import static com.futurice.freesound.feature.search.SearchConstants.SearchResultListItems.SOUND;

@Module(includes = BaseFragmentModule.class)
public class SearchFragmentModule {

    private final SearchFragment searchFragment;

    public SearchFragmentModule(SearchFragment searchFragment) {
        this.searchFragment = searchFragment;
    }

    @Provides
    @FragmentScope
    SearchFragmentViewModel provideSearchFragmentViewModel(SearchService searchService,
                                                           Navigator navigator,
                                                           AudioPlayer audioPlayer,
                                                           SchedulerProvider schedulerProvider,
                                                           TransitionObserver transitionObserver) {

        return ViewModelProviders.of(searchFragment, new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull final Class<T> modelClass) {
                return (T) new SearchFragmentViewModel(searchService, navigator, audioPlayer, schedulerProvider, transitionObserver);
            }
        }).get(SearchFragmentViewModel.class);

    }

    @Provides
    @FragmentScope
    Binder<SearchFragmentEvent, SearchFragmentState, SearchFragmentViewModel> provideBinder(
            SearchFragmentViewModel viewModel) {
        return new Binder<>(searchFragment, viewModel, searchFragment);
    }

    @Provides
    @FragmentScope
    RecyclerViewAdapter<Sound> provideRecyclerAdapter(ItemComparator itemComparator,
                                                      Map<Integer, ViewHolderFactory> factoryMap,
                                                      Map<Integer, ViewHolderBinder<Sound>> binderMap,
                                                      SchedulerProvider schedulerProvider) {
        return new RecyclerViewAdapter<>(itemComparator, factoryMap, binderMap,
                schedulerProvider);
    }

    @Provides
    ItemComparator provideComparator() {
        return new SearchResultItemComparator();
    }

    @IntoMap
    @IntKey(SOUND)
    @Provides
    ViewHolderFactory provideSoundViewHolderFactory(@ForActivity Context context,
                                                    Picasso picasso,
                                                    SchedulerProvider schedulerProvider) {
        return new SoundItemViewHolder.SoundItemViewHolderFactory(context,
                picasso,
                schedulerProvider);
    }

    @IntoMap
    @IntKey(SOUND)
    @Provides
    ViewHolderBinder<Sound> provideSoundViewHolderBinder(
            SoundItemViewModelFactory viewModelFactory) {
        return new SoundItemViewHolder.SoundItemViewHolderBinder(viewModelFactory);
    }
}
