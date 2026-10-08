package com.intu.taxi.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The visible Home map owns selection; navigation cannot leave a picker on another screen. */
object TestLocationMapSelection {
    private var owner: Any? = null
    private var startSelection: (() -> Unit)? = null
    private val active = MutableStateFlow(false)
    val picking = active.asStateFlow()

    fun register(map: Any, onStart: () -> Unit) {
        if (owner !== map) cancel()
        owner = map
        startSelection = onStart
    }
    fun unregister(map: Any) {
        if (owner === map) { owner = null; startSelection = null; cancel() }
    }
    fun request(): Boolean {
        if (owner == null) return false
        if (active.value) return true
        startSelection?.invoke()
        active.value = true
        return true
    }
    fun cancel() { active.value = false }
}
