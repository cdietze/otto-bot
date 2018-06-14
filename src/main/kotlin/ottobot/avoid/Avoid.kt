package ottobot.avoid

import ottobot.*

/**
 * Solution for the avoid mission
 * Note: works really bad
 */

fun main(args: Array<String>) {
    println("Running mode 'avoid'")
    var state: State = State.Avoid
    var ctx = StateContext()
    val moveFun: MoveFun = { map, turn ->
        ctx = ctx.copy(view = map)
        ctx = ctx.copy(move = ctx.move + 1)
        Thread.sleep(100)
        val response = state.move(ctx)
        println("ctx: $ctx, state: $state, command: ${response.first}, canMoveForward: ${ctx.view.canMoveForward()}")
        state = response.second
        ctx = when (response.first) {
            FORWARD -> ctx.copy(vec = ctx.vec + ctx.dir.toVec())
            BACKWARD -> ctx.copy(vec = ctx.vec - ctx.dir.toVec())
            LEFT -> ctx.copy(dir = ctx.dir.left())
            RIGHT -> ctx.copy(dir = ctx.dir.right())
            else -> ctx
        }
        response.first
    }
    runBot(moveFun)
}

sealed class State {
    object Avoid : State() {
        override fun move(ctx: StateContext): Pair<Command, State> {
            println("forwardThreats=${ctx.view.forwardThreats()}, backwardThreats=${ctx.view.backwardThreats()}")
            return when {
                ctx.view.forwardThreats() > ctx.view.backwardThreats() -> Pair(BACKWARD, this)
                else -> Pair(FORWARD, this)
            }
        }
    }

    abstract fun move(ctx: StateContext): Pair<Command, State>
}

fun BotMap.forwardThreats(): Int =
        asteroidCount(listOf(Vec(-1, 0), Vec(-1, -1), Vec(-1, -2), Vec(0, -2), Vec(1, -2), Vec(1, -1), Vec(1, 0)))

fun BotMap.backwardThreats(): Int =
        asteroidCount(listOf(Vec(-1, 0), Vec(-1, 1), Vec(-1, 2), Vec(0, 2), Vec(1, 2), Vec(1, 1), Vec(1, 0)))

fun BotMap.asteroidCount(vecs: List<Vec>): Int {
    val dim = dim()
    return vecs.filter { v -> this.getOrNull(dim.height / 2 + v.y)?.getOrNull(dim.width / 2 + v.x) == 'X' }.count()
}

/**
 * @param view the current view of the map
 * @param vec the current position of the bot
 * @param dir the current direction of the bot
 */
data class StateContext(val move: Int = 0, val view: BotMap = listOf(), val dir: Dir = Dir.NORTH, val vec: Vec = Vec(0, 0)) {
    override fun toString(): String =
            "StateContext(move=$move, view=$view, dir=$dir, vec=$vec)"
}
