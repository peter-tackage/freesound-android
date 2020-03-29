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

package com.futurice.freesound.arch.mvvm.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.futurice.freesound.arch.mvvm.ViewModel;

/**
 * A {@link RecyclerView.ViewHolder} which supports binding and unbinding
 * to a {@link ViewModel}.
 *
 * @param <T> {@link ViewModel} type
 */
public abstract class BindingViewHolder<T extends ViewModel> extends RecyclerView.ViewHolder {

    BindingViewHolder(final View itemView) {
        super(itemView);
    }

    public abstract void bind(@NonNull final T viewModel);

    public abstract void unbind();
}
