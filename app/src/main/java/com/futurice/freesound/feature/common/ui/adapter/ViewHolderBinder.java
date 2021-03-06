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

package com.futurice.freesound.feature.common.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.futurice.freesound.feature.common.DisplayableItem;

/**
 * Populates a {@link ViewHolder} with the model details.
 */
@FunctionalInterface
public interface ViewHolderBinder<T> {

    /**
     * Populates the passed {@link ViewHolder} with the details of the passed
     * {@link DisplayableItem}.
     */
    void bind(@NonNull final ViewHolder viewHolder, @NonNull final DisplayableItem<T> item);
}
