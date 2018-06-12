package ottobot.word

import ottobot.*
import ottobot.Command.*

/** Solution for the word mission */

const val VIEW_DIST = 5

fun main(args: Array<String>) {
    println("Running mode 'word'")
    var state: State = State.Spiral()
    var ctx = StateContext()
    val moveFun: MoveFun = { map, turn ->
        ctx = ctx.copy(view = map)
        val response = move(ctx, turn, map, state)
        state = response.second
        ctx = when (response.first) {
            FORWARD -> ctx.copy(vec = ctx.vec + ctx.dir.toVec())
            BACKWARD -> ctx.copy(vec = ctx.vec - ctx.dir.toVec())
            LEFT -> ctx.copy(dir = ctx.dir.left())
            RIGHT -> ctx.copy(dir = ctx.dir.left())
        }
        response.first
    }
    runBot(moveFun)
}

fun move(ctx: StateContext, turn: Int, view: BotMap, s: State): Pair<Command, State> {
    Thread.sleep(100)
    println("ctx: $ctx")

    TODO("Check for any letters and store them, decide whether to search for more letters at start or end, if word complete, post it")
//    val exit = findObject(view, 'O')
//    if (exit != null) {
//        println("Turn $turn, I can see the exit!")
//        return Pair(moveTowards(exit), s)
//    }
    return s.move(ctx)
}


sealed class State {
    data class Spiral(
            val stepsTaken: Int = 0, val turnsTaken: Int = 0) : State() {
        val edgeLength = ((turnsTaken / 2) + 1) * VIEW_DIST
        override fun move(ctx: StateContext): Pair<Command, State> =
                if (stepsTaken >= edgeLength) Pair(LEFT, Spiral(0, turnsTaken + 1))
                else Pair(FORWARD, copy(stepsTaken = stepsTaken + 1))
    }

    abstract fun move(ctx: StateContext): Pair<Command, State>
}

data class StateContext(val view: BotMap = listOf(), val dir: Dir = Dir.NORTH, val vec: Vec = Vec(0, 0))
