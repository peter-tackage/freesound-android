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

package com.futurice.freesound.feature.user

import com.futurice.freesound.network.api.FreeSoundApiClient
import com.futurice.freesound.network.api.model.User
import com.futurice.freesound.store.Store
import io.reactivex.Observable
import io.reactivex.Single

/*
 * refresh: Active, Single: Always fetches, stores and emits once. Can error.
 * get: Active, Single: Returns cache if it exists, otherwise it fetches, stores and emits once. Can error.
 * await: Passive, Single TODO
 * awaitStream Passive, Observable, Stream. Doesn't fetch. Just Observes. Never errors.
 *
 *  WONTDO getStream: Active, Observable: Returns cache if it exists, fetches, stores and emits value and future values.
 * Question: Should we just return the fetched value or always use the value in the store?
 * By emitting the fetched value, we are assuming that the store does not alter that.
 */
class UserRepository(private val freeSoundApi: FreeSoundApiClient,
                     private val userStore: Store<String, User>) {

    // refresh.
    fun refreshUser(username: String): Single<User> {
        return freeSoundApi.getUser(username)
                .flatMap { user -> userStore.put(username, user).toSingle { user } } // emits fetched/stored.
    }

    // get
    fun user(username: String): Single<User> {
        return userStore.get(username)
                .switchIfEmpty(refreshUser(username)) // emits fetched/stored
    }

    // awaitUserStream
    fun awaitUserStream(username: String): Observable<User> {
        return userStore.getStream(username)
    }

}
