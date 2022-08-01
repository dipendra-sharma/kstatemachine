package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.verifySequence

class JoiceStateTest: StringSpec({
    "redirecting choice state" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            logger = StateMachine.Logger { println(it) }

            val choice = choiceState("choice") { State2 }

            addInitialState(State1) {
                transition<SwitchEvent> {
                    targetState = choice
                    onTriggered { this@addInitialState.machine.log { it.toString() } }
                }
            }
            addState(State2) { callbacks.listen(this) }
            onTransition { log { it.toString() } }
        }

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onEntryState(State2) }
    }

    "choice state with argument" {
        TODO()
    }

    "redirecting choice states chain" {
        val callbacks = mockkCallbacks()

        val machine = createStateMachine {
            val choice2 = choiceState("choice2") { State2 }
            val choice1 = choiceState("choice1") { choice2 }

            addInitialState(State1) {
                transition<SwitchEvent> { targetState = choice1 }
            }
            addState(State2) { callbacks.listen(this) }
        }

        machine.processEvent(SwitchEvent)

        verifySequence { callbacks.onEntryState(State2) }
    }

    "initial choice state" {
        val callbacks = mockkCallbacks()

        createStateMachine {
            val choice = choiceState("choice") { State2 }
            setInitialState(choice)

            addState(State2) { callbacks.listen(this) }
        }

        verifySequence { callbacks.onEntryState(State2) }
    }
}) {
    private object State1 : DefaultState()
    private object State2 : DefaultState()
}