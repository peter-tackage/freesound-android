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

package com.futurice.freesound.feature.home.user

import androidx.annotation.VisibleForTesting
import com.futurice.freesound.feature.common.streams.Operation
import com.futurice.freesound.feature.common.streams.asOperation
import com.futurice.freesound.feature.user.UserRepository
import io.reactivex.Observable

class RefreshInteractor(private val userRepository: UserRepository) {

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val HOME_USERNAME = "SpiceProgram"
    }

    /**
     * Refreshes the contents of the home user
     */
    fun refresh(): Observable<Operation> {
        // Ignore the returned value, let homeUserStream emit the change.
        return refreshUser().asOperation()
    }

    private fun refreshUser() = userRepository.refreshUser(HOME_USERNAME)

}
