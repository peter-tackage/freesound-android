/*
 * Copyright 2018 Futurice GmbH
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

package com.futurice.freesound.arch.mvi.view

import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.ActionMode
import com.futurice.freesound.arch.core.BaseActivity
import com.futurice.freesound.arch.core.BaseFragment
import com.futurice.freesound.arch.mvi.viewmodel.MviViewModel
import javax.inject.Inject

/**
 * A base Activity which provides the binding mechanism hooks to a MviView Model.
 *
 * @param <C> The DI component class.
 */
abstract class MviBaseActivity<C, E, S, VM : MviViewModel<E, S>> : BaseActivity<C>(), MviView<E, S> {

    @Inject
    internal lateinit var binder: Binder<E, S, VM>

    @CallSuper
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}
