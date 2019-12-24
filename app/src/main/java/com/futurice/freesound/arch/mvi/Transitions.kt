package com.futurice.freesound.arch.mvi

interface TransitionObserver {

    fun onTransition(tag: String, transition: Transition)
}

// FIXME This needs to represent the simple case and be expanded
sealed class Transition {
    data class Event(val event: Any) : Transition()
    data class Action(val action: Any) : Transition()
    data class Result(val result: Any) : Transition()
    data class Reduce(val result: Any, val prevState: Any) : Transition()
    data class State(val state: Any) : Transition()
    data class Error(val throwable: Throwable) : Transition()
    object Completed : Transition()
}
