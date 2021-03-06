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

import com.futurice.freesound.arch.mvvm.DataBinder;
import com.futurice.freesound.arch.mvvm.ViewModel;

import io.reactivex.disposables.CompositeDisposable;

import static com.futurice.freesound.common.utils.Preconditions.checkNotNull;
import static com.futurice.freesound.common.utils.Preconditions.get;

/**
 * Provides the base operations for a binding {@link RecyclerView.ViewHolder}
 * <p>
 * Specific handling is required to support recycling.
 */
public abstract class BaseBindingViewHolder<T extends ViewModel>
        extends BindingViewHolder<T> {

    private T viewModel;

    @NonNull
    private final CompositeDisposable disposables = new CompositeDisposable();

    protected BaseBindingViewHolder(@NonNull final View view) {
        super(get(view));
    }

    @Override
    public final void bind(@NonNull final T viewModel) {
        setAndBindDataModel(get(viewModel));
        bindViewToViewModel();
    }

    @Override
    public final void unbind() {
        unbindViewFromViewModel();
        unbindViewModelFromData();
    }

    @NonNull
    protected abstract DataBinder getViewDataBinder();

    protected final T getViewModel() {
        return get(viewModel);
    }

    private void bindViewToViewModel() {
        getViewDataBinder().bind(disposables);
    }

    private void setAndBindDataModel(@NonNull final T viewModel) {
        this.viewModel = viewModel;
        viewModel.bindToDataModel();
    }

    private void unbindViewFromViewModel() {
        // Don't dispose - we need to reuse it when recycling!
        disposables.clear();
        getViewDataBinder().unbind();
    }

    private void unbindViewModelFromData() {
        checkNotNull(viewModel, "View Model cannot be null when unbinding");
        viewModel.unbindDataModel();
        viewModel = null;
    }

}
