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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.futurice.freesound.feature.audio.AudioPlayer;
import com.futurice.freesound.feature.common.DisplayableItem;
import com.futurice.freesound.feature.common.Navigator;
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider;
import com.futurice.freesound.feature.common.ui.adapter.MultiItemListAdapter;
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderBinder;
import com.futurice.freesound.feature.common.ui.adapter.ViewHolderFactory;
import com.futurice.freesound.inject.activity.ForActivity;
import com.futurice.freesound.inject.fragment.BaseFragmentModule;
import com.futurice.freesound.inject.fragment.FragmentScope;
import com.futurice.freesound.network.api.FreeSoundApiClient;
import com.futurice.freesound.network.api.model.Sound;
import com.squareup.picasso.Picasso;

import java.util.Map;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntKey;
import dagger.multibindings.IntoMap;

import static com.futurice.freesound.feature.search.SearchResultListItems.SOUND;

@Module(includes = BaseFragmentModule.class)
public class SearchFragmentModule {

    @Provides
    @FragmentScope
    static SearchFragmentViewModel provideSearchFragmentViewModel(SearchDataModel searchDataModel,
                                                                  Navigator navigator,
                                                                  AudioPlayer audioPlayer,
                                                                  SchedulerProvider schedulerProvider) {
        return new SearchFragmentViewModel(searchDataModel, navigator, audioPlayer, schedulerProvider);
    }

    @Provides
    @FragmentScope
    MultiItemListAdapter<Sound> provideRecyclerAdapter(DiffUtil.ItemCallback<DisplayableItem<Sound>> diffItemCallback,
                                                       Map<Integer, ViewHolderFactory> factoryMap,
                                                       Map<Integer, ViewHolderBinder<Sound>> binderMap) {
        return new MultiItemListAdapter<>(diffItemCallback, factoryMap, binderMap);
    }

    @Provides
    DiffUtil.ItemCallback<DisplayableItem<Sound>> provideDiffItemCallback() {
        return new DiffUtil.ItemCallback<DisplayableItem<Sound>>() {
            @Override
            public boolean areItemsTheSame(@NonNull DisplayableItem<Sound> oldItem,
                                           @NonNull DisplayableItem<Sound> newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DisplayableItem<Sound> oldItem,
                                              @NonNull DisplayableItem<Sound> newItem) {
                // No local state.
                return true;
            }
        };
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

    @Provides
    SoundItemViewModelFactory provideSoundViewModelFactory(Navigator navigator,
                                                           AudioPlayer audioPlayer,
                                                           FreeSoundApiClient freeSoundApiClient) {
        return new SoundItemViewModelFactory(navigator, audioPlayer, freeSoundApiClient);
    }

    @IntoMap
    @IntKey(SOUND)
    @Provides
    ViewHolderBinder<Sound> provideSoundViewHolderBinder(
            SoundItemViewModelFactory viewModelFactory) {
        return new SoundItemViewHolder.SoundItemViewHolderBinder(viewModelFactory);
    }
}
