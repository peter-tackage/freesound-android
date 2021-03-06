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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.fragment.app.FragmentActivity

// Fragment
@Suppress("UNCHECKED_CAST")
inline fun <reified VM : ViewModel> androidx.fragment.app.Fragment.viewModelProvider(
        crossinline provider: () -> VM): VM {
    return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
        override fun <T1 : ViewModel> create(aClass: Class<T1>) = provider() as T1
    }).get(VM::class.java)
}

// Activity
@Suppress("UNCHECKED_CAST")
inline fun <reified VM : ViewModel> FragmentActivity.viewModelProvider(
        crossinline provider: () -> VM): VM {
    return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
        override fun <T1 : ViewModel> create(aClass: Class<T1>) = provider() as T1
    }).get(VM::class.java)
}
