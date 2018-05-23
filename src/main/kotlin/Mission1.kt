/** Solution for the escape mission */

// we know the map is 32x32 and the view is 5x5,
// so we move forward 32-5 steps and then move 5 orthogonally
const val FORWARD_COUNT = 32 - 5
const val SIDESTEP_COUNT = 5

object Mission1 {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hi from ${this.javaClass}")
        var state: State = State.Forward(0)
        val moveFun: MoveFun = { map, turn ->
            val response = move(turn, map, state)
            state = response.second
            response.first
        }
        runBot(moveFun)
    }
}

fun move(turn: Int, view: List<String>, s: State): Pair<Char, State> {
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
        override fun move(): Pair<Char, State> =
                if (steps == 0) Pair('<', Orthogonal())
                else Pair('^', copy(steps = steps - 1))
    }

    data class Orthogonal(
            val steps: Int = SIDESTEP_COUNT) : State() {
        override fun move(): Pair<Char, State> =
                if (steps == 0) Pair('>', Forward())
                else Pair('^', copy(steps = steps - 1))
    }

    abstract fun move(): Pair<Char, State>
}
