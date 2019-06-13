package ottobot.word

import ottobot.*

/**
 * Solution for the word mission
 *
 * Known issues:
 * - Fails when a letter is found after looping the world - these will be considered separate words and will never pass,
 *   however this should not occur since when the first letter is found, the bot tries to explore the local word
 * - If the bot would approach the word front on, it would keep moving forward to explore the word and thus be blocked
 *   forever
 */

fun main() {
    println("Running mode 'word'")
    var state: State = State.ExploreWord()
    var ctx = StateContext()
    val moveFun: MoveFun = { map, move ->
        ctx = ctx.copy(view = map, move = move)
        ctx = ctx.copy(knownMap = updateKnownMap(ctx))
        ctx = ctx.copy(move = ctx.move + 1)
        Thread.sleep(10)
        val response = state.move(ctx) ?: error("State did not produce a command!, state: $state")
        println("ctx: $ctx, state: $state, command: ${response.first}, canMoveForward: ${ctx.view.canMoveForward()}")
        state = response.second
        ctx = when (response.first) {
            FORWARD -> ctx.copy(pos = ctx.pos + ctx.dir.toVec())
            BACKWARD -> ctx.copy(pos = ctx.pos - ctx.dir.toVec())
            LEFT -> ctx.copy(dir = ctx.dir.left())
            RIGHT -> ctx.copy(dir = ctx.dir.right())
            else -> ctx
        }
        response.first
    }
    runBot(moveFun)
}

fun updateKnownMap(ctx: StateContext): KnownMap =
        ctx.knownMap.toMutableMap().apply {
            putAll(ctx.view
                    .zipWithVec()
                    .filterValues { !it.isUpperCase() }
                    .mapKeys { it.key.alignToNorth(ctx.dir) + ctx.pos }
            )
        }

fun tryToFindWord(ctx: StateContext, next: State): State? {
    val letters: List<Pair<Vec, Char>> = ctx.knownMap.filterValues { it.isLetter() && it.isLowerCase() }.toList().sortedBy { it.first }
    if (letters.isEmpty()) return null
    if (letters.size == 1) return State.Explore(letters.first().first.neighbors(), next)
    val letterVec = letters[1].first - letters[0].first
    val leadingVec = letters.first().first - letterVec
    val trailingVec = letters.last().first + letterVec
    val openEnds = listOf(leadingVec, trailingVec).filter { !ctx.knownMap.containsKey(it) }
    val word = letters.map { it.second }.joinToString("")
    println("tryToFindWord: word=$word, length=${word.length}, letterVec=$letterVec, letters=$letters")
    return if (openEnds.isEmpty()) State.EnterWord(word) else State.Explore(openEnds, next)
}

fun tryExplore(ctx: StateContext, targets: List<Vec>): Command? {
    val dim = ctx.view.dim()
    val unexplored = targets.filter { !ctx.knownMap.containsKey(it) }
    println("unexplored=$unexplored")
    return unexplored.map { it - ctx.pos }.sortedBy { x -> x.movesAway() }.firstOrNull()?.let { moveCloseTo(dim, it, ctx.dir) }
}

sealed class State {

    data class Spiral(val stepsTaken: Int = 0, val turnsTaken: Int = 0) : State() {
        override fun move(ctx: StateContext): Pair<Command, State> {
            val edgeLength = ((turnsTaken / 2) + 1) * ctx.view.dim().width
            return if (stepsTaken >= edgeLength || !ctx.view.canMoveForward()) Pair(LEFT, Spiral(0, turnsTaken + 1))
            else Pair(FORWARD, copy(stepsTaken = stepsTaken + 1))
        }
    }

    data class EnterWord(val word: String, val index: Int = 0) : State() {
        override fun move(ctx: StateContext): Pair<Command, State> {
            if (index >= word.length) return EnterWord(word.reversed()).move(ctx)
            return Pair(word[index], this.copy(index = index + 1))
        }
    }

    data class Explore(val targets: List<Vec>, val next: State) : State() {
        override fun move(ctx: StateContext): Pair<Command, State>? {
            return tryExplore(ctx, targets)?.let { Pair(it, this) } ?: next.move(ctx)
        }
    }

    data class ExploreWord(val spiral: State = Spiral()) : State() {
        override fun move(ctx: StateContext): Pair<Command, State>? {
            return tryToFindWord(ctx, this)?.move(ctx)
                    ?: progressSpiral(ctx)
        }

        private fun progressSpiral(ctx: StateContext): Pair<Command, State>? {
            val r = spiral.move(ctx)
            return r?.let { Pair(it.first, copy(spiral = it.second)) }
        }
    }

    abstract fun move(ctx: StateContext): Pair<Command, State>?
}

/**
 * @param view the current view of the map
 * @param pos the current position of the bot
 * @param dir the current direction of the bot
 */
data class StateContext(val move: Int = 0, val view: BotMap = listOf(), val knownMap: KnownMap = mapOf(), val dir: Dir = Dir.NORTH, val pos: Vec = Vec(0, 0)) {
    override fun toString(): String =
            "StateContext(move=$move, view=$view, knownMap.size=${knownMap.size}, dir=$dir, pos=$pos)"
}

typealias KnownMap = Map<Vec, Char>