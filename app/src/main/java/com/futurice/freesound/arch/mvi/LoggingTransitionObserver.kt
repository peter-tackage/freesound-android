package com.futurice.freesound.arch.mvi

import timber.log.Timber

class LoggingTransitionObserver : TransitionObserver {

    override fun onTransition(tag: String, transition: Transition) {
        when (transition) {
            is Transition.Event -> Timber.d("$tag| Event => $transition")
            is Transition.Action -> Timber.d("$tag| Action => $transition")
            is Transition.Result -> Timber.d("$tag| Result => $transition")
            is Transition.Reduce -> Timber.d("$tag| Reduce => $transition")
            is Transition.State -> Timber.d("$tag| State => $transition")
            is Transition.Completed -> Timber.w("$tag| Completed => $transition")
            is Transition.Error -> Timber.e(transition.throwable, "$tag| Fatal Error => $transition")
        }
    }
}
