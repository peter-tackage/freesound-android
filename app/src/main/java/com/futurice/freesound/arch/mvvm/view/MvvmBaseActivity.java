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

package com.futurice.freesound.arch.mvvm.view;

import com.futurice.freesound.arch.mvvm.BaseLifecycleViewDataBinder;
import com.futurice.freesound.arch.mvvm.DataBinder;
import com.futurice.freesound.arch.mvvm.ViewModel;
import com.futurice.freesound.arch.core.BaseActivity;

import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.disposables.CompositeDisposable;

/**
 * A base Activity which provides the binding mechanism hooks to a View Model.
 *
 * @param <T> The DI component class.
 */
public abstract class MvvmBaseActivity<T> extends BaseActivity<T> {

    @NonNull
    private final BaseLifecycleViewDataBinder lifecycleBinder = new BaseLifecycleViewDataBinder() {

        @Override
        public void bind(@NonNull final CompositeDisposable disposables) {
            dataBinder().bind(disposables);
        }

        @Override
        public void unbind() {
            dataBinder().unbind();
        }

        @NonNull
        @Override
        public ViewModel viewModel() {
            return MvvmBaseActivity.this.viewModel();
        }

    };

    @CallSuper
    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleBinder.onCreate();
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        lifecycleBinder.onResume();
    }

    @CallSuper
    @Override
    public void onPause() {
        lifecycleBinder.onPause();
        super.onPause();
    }

    @CallSuper
    @Override
    public void onDestroy() {
        lifecycleBinder.onDestroyView();
        lifecycleBinder.onDestroy();
        super.onDestroy();
    }

    @NonNull
    protected abstract ViewModel viewModel();

    @NonNull
    protected abstract DataBinder dataBinder();
}
