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

import com.futurice.freesound.arch.mvi.Transition
import com.futurice.freesound.arch.mvi.TransitionObserver
import com.futurice.freesound.feature.common.scheduling.SchedulerProvider
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer

abstract class SimpleViewModel<E, S>(private val initialState: S,
                                     schedulerProvider: SchedulerProvider,
                                     transitionObserver: TransitionObserver,
                                     tag: String)
    : BaseViewModel<E, S>(schedulerProvider, transitionObserver, tag) {

    init {
        state.value = initialState
    }

    override fun transformEventToStateStream(events: Flowable<E>): Flowable<S> {
        return events
                .doOnNext { onTransition(Transition.Event(it as Any)) }
                .compose(transformEventToStateStream(state.value!!, transforms()))
                .doOnNext { onTransition(Transition.State(it as Any)) }
                .startWith(initialState)
                .doOnComplete { onTransition(Transition.Completed) }
    }

    //
    // TODO Need to update these to allow changes to transform based on state too.
    //

    protected inline fun <reified E2 : E, R, S2 : S> fromEventStream(transform: FlowableTransformer<in E2, out R>,
                                                                     crossinline stateUpdate: (S, R) -> S = { s, r -> s })
            : (state: S) -> FlowableTransformer<in E, out S> =
            { state: S ->
                FlowableTransformer { events ->
                    events.ofType(E2::class.java)
                            .compose(transform)
                            .map { stateUpdate(state, it) }
                }
            }

    protected inline fun <reified E2 : E, S2 : S> fromEvent(noinline stateUpdate: (S, E2) -> S)
            : (S2) -> FlowableTransformer<E, S> =
            { state: S ->
                FlowableTransformer { events ->
                    events.ofType(E2::class.java)
                            .map { stateUpdate(state, it) }
                }
            }

    protected inline fun <reified E2 : E, S2 : S> fromEventDo(noinline stateUpdate: (S, E2) -> Unit)
            : (S2) -> FlowableTransformer<E, S> =
            { state: S ->
                FlowableTransformer { events ->
                    events.ofType(E2::class.java)
                            .map { stateUpdate(state, it) }
                            .map { state }
                }
            }

    private fun <E, S> transformEventToStateStream(state: S,
                                                   transforms: List<(state: S) -> FlowableTransformer<in E, out S>>
    ): FlowableTransformer<E, S> {
        return FlowableTransformer {
            it.publish { events: Flowable<out E> ->
                Flowable.merge(transforms.map { transformer -> events.compose(transformer(state)) })
            }
        }
    }

    abstract protected fun transforms(): List<(state: S) -> FlowableTransformer<in E, out S>>

}
