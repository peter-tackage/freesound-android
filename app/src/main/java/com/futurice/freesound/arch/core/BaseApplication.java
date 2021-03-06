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

package com.futurice.freesound.arch.core;

import com.futurice.freesound.inject.Injector;

import android.app.Application;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

/**
 * A base Application which provides a dependency injection mechanism.
 *
 * @param <T> The DI component class
 */
public abstract class BaseApplication<T> extends Application implements Injector<T> {

    private T component;

    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        inject();
    }

    @NonNull
    @Override
    public T component() {
        if (component == null) {
            component = createComponent();
        }
        return component;
    }

    @NonNull
    protected abstract T createComponent();

}
