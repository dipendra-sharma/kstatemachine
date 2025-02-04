package ru.nsk.kstatemachine

import kotlin.properties.Delegates.observable

interface GroupListener {
    fun unsubscribe()
}

private interface StateGroupListener : IState.Listener, GroupListener

/**
 * Triggers [onChanged] callback when condition "all passed states are active" changes
 */
fun onActiveAllOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    val allStates = setOf(mandatoryState1, mandatoryState2, *otherStates)
    require(allStates.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }
    val initialActiveCount = allStates.countActive()

    val listener = object : StateGroupListener {
        private var status by observable(initialActiveCount == allStates.size) { _, oldValue, newValue ->
            if (oldValue != newValue) onChanged(newValue)
        }

        private var activeCount = initialActiveCount
            set(value) {
                field = value
                status = allStates.countActive() == allStates.size
            }

        init {
            if (notifyOnSubscribe) onChanged(status)
        }

        override fun onEntry(transitionParams: TransitionParams<*>) {
            ++activeCount
        }

        override fun onExit(transitionParams: TransitionParams<*>) {
            --activeCount
        }

        override fun unsubscribe() {
            allStates.forEach { it.removeListener(this) }
        }
    }

    allStates.forEach { it.addListener(listener) }
    return listener
}

private fun Iterable<IState>.countActive() = count { it.isActive }

/**
 * Triggers [onChanged] callback when condition "any of passed states is active" changes
 */
fun onActiveAnyOf(
    mandatoryState1: IState,
    mandatoryState2: IState,
    vararg otherStates: IState,
    notifyOnSubscribe: Boolean = false,
    onChanged: (Boolean) -> Unit
): GroupListener {
    val allStates = setOf(mandatoryState1, mandatoryState2, *otherStates)
    require(allStates.size >= 2) {
        "There is no sense to use this API with less than 2 unique states, did you passed same state more then once?"
    }

    val listener = object : StateGroupListener {
        private var status by observable(calculateStatus()) { _, oldValue, newValue ->
            if (oldValue != newValue) onChanged(newValue)
        }

        init {
            if (notifyOnSubscribe) onChanged(status)
        }

        private fun updateStatus() {
            status = calculateStatus()
        }

        private fun calculateStatus() = allStates.firstOrNull { it.isActive } != null

        override fun onEntry(transitionParams: TransitionParams<*>) = updateStatus()
        override fun onExit(transitionParams: TransitionParams<*>) = updateStatus()

        override fun unsubscribe() {
            allStates.forEach { it.removeListener(this) }
        }
    }

    allStates.forEach { it.addListener(listener) }
    return listener
}