package ottobot.word

import ottobot.BACKWARD
import ottobot.BotMap
import ottobot.Command
import ottobot.Dir
import ottobot.FORWARD
import ottobot.LEFT
import ottobot.MoveFun
import ottobot.Node
import ottobot.RIGHT
import ottobot.Vec
import ottobot.alignToNorth
import ottobot.breadthFirst
import ottobot.canMoveForward
import ottobot.command
import ottobot.left
import ottobot.manhattanDistance
import ottobot.minus
import ottobot.neighbors
import ottobot.plus
import ottobot.right
import ottobot.runBot
import ottobot.toVec
import ottobot.viewRadius
import ottobot.zipWithVec
import java.util.Random

/**
 * Solution for the word mission
 *
 * Works on `plain` and `random` (with obstacles) terrain.
 *
 * Start the server for example using `./bots -G -1 -V 1 -t random word`
 */

val random = Random(1)

fun main() {
    println("Running mode 'word'")
    var state: State = State.Explore
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
        putAll(
            ctx.view
                .zipWithVec()
                .mapValues { if (it.value.isUpperCase()) '.' else it.value }
                .mapKeys { it.key.alignToNorth(ctx.dir) + ctx.pos }
        )
    }

fun tryToExposeWord(ctx: StateContext, next: State): Pair<Command, State>? {
    val letters: List<Pair<Vec, Char>> =
        ctx.knownMap.filterValues { it.isLetter() && it.isLowerCase() }.toList().sortedBy { it.first }
    if (letters.isEmpty()) return null
    if (letters.size == 1) return tryExploreAnyTarget(ctx, letters.first().first.neighbors())?.let { Pair(it, next) }
    val letterVec = letters[1].first - letters[0].first
    val leadingVec = letters.first().first - letterVec
    val trailingVec = letters.last().first + letterVec
    val openEnds = listOf(leadingVec, trailingVec).filter { !ctx.knownMap.containsKey(it) }.toSet()
    val word = letters.map { it.second }.joinToString("")
    println("tryToFindWord: word=$word, length=${word.length}, letterVec=$letterVec, letters=$letters, openEnds:$openEnds")
    return if (openEnds.isEmpty()) State.EnterWord(word).move(ctx)
    else tryExploreAnyTarget(ctx, openEnds)?.let { Pair(it, next) }
}

fun tryExploreAnyTarget(ctx: StateContext, targets: Set<Vec>): Command? {
    val pathResult = breadthFirst(ctx.pos, ctx.dir, ctx.knownMap.obstacles(), ctx.view.viewRadius())
    val unexploredTargets = targets.filter { !ctx.knownMap.containsKey(it) }
    println("unexploredTargets=$unexploredTargets")
    val bestPath: List<Node>? = unexploredTargets.map { t -> Pair(t, pathResult.path(t)) }.minByOrNull { p ->
        p.second?.size ?: Int.MAX_VALUE
    }?.second
    return bestPath?.firstCommand()
}

fun tryExplore(ctx: StateContext): Command? {
    val pathResult = breadthFirst(ctx.pos, ctx.dir, ctx.knownMap.obstacles(), ctx.view.viewRadius())
    val targets: Set<Vec> =
        ctx.knownMap.keys.flatMap { it.neighbors() }.filter { !ctx.knownMap.containsKey(it) }.toSet()
    val bestPath = targets.map { t ->
        Pair(t, pathResult.path(t))
    }.minByOrNull { p ->
        p.second?.size?.let { moves -> moves + p.first.manhattanDistance() * 0.3f } ?: Float.MAX_VALUE
    }?.second
    return bestPath?.firstCommand()
}

fun List<Node>.firstCommand(): Command {
    require(this.size >= 2)
    return command(this[this.size - 2], this[this.size - 1])
}

sealed class State {

    object Explore : State() {
        override fun move(ctx: StateContext): Pair<Command, State> =
            tryToExposeWord(ctx, this)
                ?: tryExplore(ctx)?.let { Pair(it, this) }
                ?: Pair(randomCommand(), this)
    }

    data class EnterWord(val word: String, val index: Int = 0) : State() {
        override fun move(ctx: StateContext): Pair<Command, State> =
            if (index >= word.length) EnterWord(word.reversed()).move(ctx)
            else Pair(word[index], this.copy(index = index + 1))
    }

    abstract fun move(ctx: StateContext): Pair<Command, State>?
}

/**
 * @param view the current view of the map
 * @param pos the current position of the bot
 * @param dir the current direction of the bot
 */
data class StateContext(
    val move: Int = 0,
    val view: BotMap = listOf(),
    val knownMap: KnownMap = mapOf(),
    val dir: Dir = Dir.NORTH,
    val pos: Vec = Vec(0, 0)
) {
    override fun toString(): String =
        "StateContext(move=$move, view=$view, knownMap.size=${knownMap.size}, dir=$dir, pos=$pos)"
}

typealias KnownMap = Map<Vec, Char>

fun KnownMap.obstacles(): Set<Vec> = filterValues { it != '.' }.keys

data class Move(val command: Command, val node: Node)

fun randomCommand(): Command = when (random.nextInt(4)) {
    0 -> '<'
    1 -> '>'
    2 -> '^'
    else -> 'v'
}
