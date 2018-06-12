package escape

import BotMap
import Command
import Command.*
import MoveFun
import findObject
import moveTowards
import runBot

/** Solution for the escape mission */

// we know the map is 32x32 and the view is 5x5,
// so we escape.move forward 32-5 steps and then escape.move 5 orthogonally
const val FORWARD_COUNT = 32 - 5
const val SIDESTEP_COUNT = 5

fun main(args: Array<String>) {
    println("Running mode 'escape'")
    var state: State = State.Forward(0)
    val moveFun: MoveFun = { map, turn ->
        val response = move(turn, map, state)
        state = response.second
        response.first
    }
    runBot(moveFun)
}

fun move(turn: Int, view: BotMap, s: State): Pair<Command, State> {
    val exit = findObject(view, 'O')
    if (exit != null) {
        println("Turn $turn, I can see the exit!")
        return Pair(moveTowards(exit), s)
    }
    return s.move()
}

sealed class State {
    data class Forward(
            val steps: Int = FORWARD_COUNT) : State() {
        override fun move(): Pair<Command, State> =
                if (steps == 0) Pair(LEFT, Orthogonal())
                else Pair(FORWARD, copy(steps = steps - 1))
    }

    data class Orthogonal(
            val steps: Int = SIDESTEP_COUNT) : State() {
        override fun move(): Pair<Command, State> =
                if (steps == 0) Pair(RIGHT, Forward())
                else Pair(FORWARD, copy(steps = steps - 1))
    }

    abstract fun move(): Pair<Command, State>
}
