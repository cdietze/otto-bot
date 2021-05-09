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
import java.util.PriorityQueue
import java.util.Random
import kotlin.math.max

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
    var state: State = State.ExploreWord
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

fun tryToFindWord(ctx: StateContext, next: State): State? {
    val letters: List<Pair<Vec, Char>> =
        ctx.knownMap.filterValues { it.isLetter() && it.isLowerCase() }.toList().sortedBy { it.first }
    if (letters.isEmpty()) return null
    if (letters.size == 1) return State.Explore(letters.first().first.neighbors(), next)
    val letterVec = letters[1].first - letters[0].first
    val leadingVec = letters.first().first - letterVec
    val trailingVec = letters.last().first + letterVec
    val openEnds = listOf(leadingVec, trailingVec).filter { !ctx.knownMap.containsKey(it) }
    val word = letters.map { it.second }.joinToString("")
    println("tryToFindWord: word=$word, length=${word.length}, letterVec=$letterVec, letters=$letters, openEnds:$openEnds")
    return if (openEnds.isEmpty()) State.EnterWord(word) else State.Explore(openEnds, next)
}

fun tryExplore(ctx: StateContext, targets: List<Vec>): Command? {
    val viewRadius = ctx.view.viewRadius()
    val unexploredTargets = targets.filter { !ctx.knownMap.containsKey(it) }
    println("unexploredTargets=$unexploredTargets")
    if (unexploredTargets.isEmpty()) {
        return null
    }
    val obstacles = ctx.knownMap.filterValues { it != '.' }.keys
    val path = shortestPathToView(ctx.pos, ctx.dir, unexploredTargets, obstacles, viewRadius)
    println("#tryExplore, path: $path")
    if (path != null) {
        return path.first()
    }
    return randomCommand()
}

sealed class State {

    data class EnterWord(val word: String, val index: Int = 0) : State() {
        override fun move(ctx: StateContext): Pair<Command, State> {
            if (index >= word.length) return EnterWord(word.reversed()).move(ctx)
            return Pair(word[index], this.copy(index = index + 1))
        }
    }

    data class Explore(val targets: List<Vec>, val next: State) : State() {
        override fun move(ctx: StateContext): Pair<Command, State>? {
            return tryExplore(ctx, targets)?.let { Pair(it, next) } ?: next.move(ctx)
        }
    }

    object ExploreWord : State() {
        override fun move(ctx: StateContext): Pair<Command, State>? {
            return tryToFindWord(ctx, this)?.move(ctx)
                ?: Pair(explore(ctx), this)
        }

        private fun explore(ctx: StateContext): Command {
            val obstacles = ctx.knownMap.filterValues { it != '.' }.keys

            val result = breadthFirst(ctx.pos, ctx.dir, obstacles, ctx.view.viewRadius())

            val targets: Set<Vec> =
                ctx.knownMap.keys.flatMap { it.neighbors() }.filter { !ctx.knownMap.containsKey(it) }.toSet()

            val bestPair: Pair<Vec, List<Node>?>? = targets.map { t ->
                val path = result.path(t)
                Pair(t, path)
            }.minByOrNull {
                it.second?.size?.let { moves -> moves + it.first.manhattanDistance() * 0.3f } ?: Float.MAX_VALUE
            }

            val path = bestPair?.second
            val c = path?.let { p -> command(p[p.size - 2], p[p.size - 1]) }
            return c ?: randomCommand()
        }
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

data class Node(val pos: Vec, val dir: Dir)

// typealias Move = Pair<Command, Node>
data class Move(val command: Command, val node: Node)

fun Node.moves(): List<Move> = listOf(
    Move('<', this.copy(dir = dir.left())),
    Move('>', this.copy(dir = dir.right())),
    Move('^', this.copy(pos = pos + dir.toVec())),
    Move('v', this.copy(pos = pos - dir.toVec()))
)

fun randomCommand(): Command = when (random.nextInt(4)) {
    0 -> '<'
    1 -> '>'
    2 -> '^'
    else -> 'v'
}

val maxCost = 100

fun shortestPathToView(pos: Vec, dir: Dir, targets: List<Vec>, obstacles: Set<Vec>, viewRadius: Int): List<Command>? {

    println("#shortestPathToView pos:$pos, dir:$dir, targets:$targets")

    require(!targets.isEmpty())

    //    fun heuristic(target: Vec, n: Node): Int = (target - n.pos).manhattanDistance()
    fun heuristic(target: Vec, n: Node): Int {
        val v = target - n.pos
        val x = max(0, v.x - viewRadius - 1)
        val y = max(0, v.y - viewRadius - 1)
        val offDir = if (n.dir == Dir.EAST || n.dir == Dir.WEST) y else x
        return x + y + if (offDir > 0) 1 else 0
    }

    val startNode = Node(pos, dir)
    val frontier = PriorityQueue<Pair<Int, Node>>(compareBy { it.first })
    frontier.add(Pair(0, startNode))
    val cameFrom = mutableMapOf<Node, Move>()
    val costSoFar = mutableMapOf<Node, Int>()
    costSoFar[startNode] = 0

    while (!frontier.isEmpty()) {
        val current: Node = frontier.poll().second
//        println("#shortestPathToView frontier:${frontier.size}, current:$current, cameFrom:${cameFrom.entries}")
        if (targets.any { (it - current.pos).manhattanDistance() <= viewRadius }) {
            val result = mutableListOf<Command>()
            var n = current
            while (true) {
                val n2 = cameFrom[n] ?: return result.reversed()
                n = n2.node
                result.add(n2.command)
            }
        }

        for (m in current.moves()) {
            if (obstacles.contains(m.node.pos)) continue
            val newCost = costSoFar[current]!! + 1
            if (newCost > maxCost) {
                println("WARN: #shortestPathToView exceeded max moves")
                return null
            }
            val seenBetter = costSoFar[m.node]?.let { newCost > it } == true
            if (!seenBetter) {
                costSoFar[m.node] = newCost
                val priority = newCost + (targets.map { heuristic(it, m.node) }.minOrNull()!!)
                frontier.add(Pair(priority, m.node))
                cameFrom[m.node] = Move(m.command, current)
            }
        }
    }

    // Targets are unreachable
    println("WARN: #shortestPathToView found no reachable target")
    return null
}
