package collect

import BotMap
import Command
import Command.*
import MoveFun
import findObject
import moveTowards
import runBot

/** Solution for the collect mode */

// we know the map is 32x32 and the view is 5x5,
// so we escape.move forward 32-5 steps and then escape.move 5 orthogonally
const val FORWARD_COUNT = 32 - 5
const val SIDESTEP_COUNT = 5

fun main(args: Array<String>) {
    println("Running mode 'collect'")
    var state: State = State.Forward(0)
    val moveFun: MoveFun = { map, turn ->
        val response = move(turn, map, state)
        state = response.second
        response.first
    }
    runBot(moveFun)
}

fun move(turn: Int, view: BotMap, s: State): Pair<Command, State> {
    //    Thread.sleep(50)
    val gem = findObject(view, '@')
    if (gem != null) {
        println("Turn $turn, I can see a gem!")
        return State.Fetch(s, 0).move(view)
    }
    return s.move(view)
}

sealed class State {
    data class Forward(
            val steps: Int = FORWARD_COUNT) : State() {
        override fun move(view: BotMap): Pair<Command, State> =
                if (steps == 0) Pair(LEFT, Orthogonal())
                else Pair(FORWARD, copy(steps = steps - 1))
    }

    data class Orthogonal(
            val steps: Int = SIDESTEP_COUNT) : State() {
        override fun move(view: BotMap): Pair<Command, State> =
                if (steps == 0) Pair(RIGHT, Forward())
                else Pair(FORWARD, copy(steps = steps - 1))
    }

    /**
     * @param rotation number of right turns. negative number means number of left turns
     */
    data class Fetch(
            val oldState: State,
            val rotation: Int) : State() {
        override fun move(view: BotMap): Pair<Command, State> {
            val gem = findObject(view, '@')
            if (gem == null) {
                if (rotation > 0) {
                    return Pair(LEFT, copy(rotation = rotation - 1))
                } else if (rotation < 0) {
                    return Pair(RIGHT, copy(rotation = rotation + 1))
                }
                return oldState.move(view)
            }
            val cmd = moveTowards(gem)
            val newRotation = rotation + when (cmd) {
                LEFT -> -1
                RIGHT -> 1
                else -> 0
            }
            return Pair(cmd, copy(rotation = newRotation))
        }
    }

    abstract fun move(view: BotMap): Pair<Command, State>
}
