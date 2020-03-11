/*
 * Copyright 2017 Futurice GmbH
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

package com.futurice.freesound.arch.mvi.viewmodel

import androidx.fragment.app.Fragment

// FIXME Not using this for now because it's too ugly in Java
// Need this bridge for now because you can't call reified functions from Java.
internal fun
        <E, S>
        Fragment.createViewModel(provider: () -> BaseViewModel<E, S>): BaseViewModel<E, S> {
    return viewModelProvider(provider)
}
