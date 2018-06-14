package ottobot.word

import ottobot.*

/**
 * Solution for the word mission
 *
 * TODO Known issues:
 * - Fails when a letter is found after looping the world - these will be considered separate words and will never pass
 */

const val VIEW_DIST = 5

fun main(args: Array<String>) {
    println("Running mode 'word'")
    var state: State = State.Spiral()
    var ctx = StateContext()
    val moveFun: MoveFun = { map, turn ->
        ctx = ctx.copy(view = map)
        ctx = ctx.copy(knownMap = updateKnownMap(ctx))
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

fun updateKnownMap(ctx: StateContext): KnownMap =
        ctx.knownMap.toMutableMap().apply {
            putAll(ctx.view
                    .zipWithVec()
                    .filterValues { !it.isUpperCase() }
                    .mapKeys { it.key.alignToNorth(ctx.dir) + ctx.vec }
            )
        }

fun tryToFindWord(ctx: StateContext): String? {
    val letters: List<Pair<Vec, Char>> = ctx.knownMap.filterValues { it.isLetter() && it.isLowerCase() }.toList().sortedBy { it.first }
    if (letters.size < 2) return null
    val letterVec = letters[1].first - letters[0].first
    val foundStart = ctx.knownMap.containsKey(letters.first().first - letterVec)
    val foundEnd = ctx.knownMap.containsKey(letters.last().first + letterVec)
    val word = letters.map { it.second }.joinToString("")
    println("tryToFindWord: word=$word, length=${word.length}, letterVec=$letterVec, letters=$letters")
    return if (foundStart && foundEnd) word else null
}

sealed class State {
    data class Spiral(
            val stepsTaken: Int = 0, val turnsTaken: Int = 0) : State() {
        val edgeLength = ((turnsTaken / 2) + 1) * VIEW_DIST
        override fun move(ctx: StateContext): Pair<Command, State> =
                tryToFindWord(ctx)?.let {
                    println("I found the word: '$it' (or '${it.reversed()}')")
                    EnterWord(it).move(ctx)
                } ?: if (stepsTaken >= edgeLength || !ctx.view.canMoveForward()) Pair(LEFT, Spiral(0, turnsTaken + 1))
                else Pair(FORWARD, copy(stepsTaken = stepsTaken + 1))
    }

    data class EnterWord(val word: String, val index: Int = 0) : State() {
        override fun move(ctx: StateContext): Pair<Command, State> {
            if (index >= word.length) return EnterWord(word.reversed()).move(ctx)
            return Pair(word[index], this.copy(index = index + 1))
        }
    }

    abstract fun move(ctx: StateContext): Pair<Command, State>
}

/**
 * @param view the current view of the map
 * @param vec the current position of the bot
 * @param dir the current direction of the bot
 */
data class StateContext(val move: Int = 0, val view: BotMap = listOf(), val knownMap: KnownMap = mapOf(), val dir: Dir = Dir.NORTH, val vec: Vec = Vec(0, 0)) {
    override fun toString(): String =
            "StateContext(move=$move, view=$view, knownMap.size=${knownMap.size}, dir=$dir, vec=$vec)"
}

typealias KnownMap = Map<Vec, Char>