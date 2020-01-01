package com.futurice.freesound.arch.mvi

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer

typealias Reducer<R, S> = (S, R) -> S

typealias Dispatcher<A, R> = FlowableTransformer<in A, out R>

fun <A, R> combine(vararg transformers: FlowableTransformer<in A, out R>): FlowableTransformer<in A, out R> {
    return FlowableTransformer {
        it.publish { actions: Flowable<out A> ->
            Flowable.merge(transformers.map { it2 -> actions.compose(it2) })
        }
    }
}
