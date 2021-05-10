package ottobot

import kotlin.math.abs

data class Dim(val width: Int, val height: Int)

data class Vec(val x: Int, val y: Int) : Comparable<Vec> {
    override fun compareTo(other: Vec): Int = compareValuesBy(this, other, { it.x }, { it.y })
}

operator fun Vec.plus(v: Vec): Vec = Vec(x + v.x, y + v.y)
operator fun Vec.minus(v: Vec): Vec = Vec(x - v.x, y - v.y)

fun Vec.neighbors(): Set<Vec> = setOf(Vec(x + 1, y), Vec(x, y + 1), Vec(x - 1, y), Vec(x, y - 1))

/** @returns the number of moves to re2ach [this]
 * (when x != 0 we need to turn once) */
fun Vec.movesAway(): Int = y + if (x == 0) 0 else 1 + x

fun Vec.manhattanDistance(): Int = abs(y) + abs(x)

fun Vec.alignToNorth(dir: Dir): Vec = when (dir) {
    Dir.NORTH -> this
    Dir.EAST -> Vec(-y, x)
    Dir.SOUTH -> Vec(-x, -y)
    Dir.WEST -> Vec(y, -x)
}

fun Vec.alignFromNorth(dir: Dir): Vec = when (dir) {
    Dir.NORTH -> this
    Dir.EAST -> Vec(y, -x)
    Dir.SOUTH -> Vec(-x, -y)
    Dir.WEST -> Vec(-y, x)
}

enum class Dir {
    NORTH,
    EAST,
    SOUTH,
    WEST
}

fun Dir.toVec(): Vec = when (this) {
    Dir.NORTH -> Vec(0, -1)
    Dir.EAST -> Vec(1, 0)
    Dir.SOUTH -> Vec(0, 1)
    Dir.WEST -> Vec(-1, 0)
}

fun Dir.left(): Dir = when (this) {
    Dir.NORTH -> Dir.WEST
    Dir.EAST -> Dir.NORTH
    Dir.SOUTH -> Dir.EAST
    Dir.WEST -> Dir.SOUTH
}

fun Dir.right(): Dir = left().left().left()

fun moveTo(vec: Vec): Command {
    return when {
        vec.y < 0 -> FORWARD
        vec.y > 0 -> BACKWARD
        vec.x < 0 -> LEFT
        else -> RIGHT
    }
}

fun moveTo(vec: Vec, myDir: Dir): Command {
    println(
        "moveTo, pos=$vec, myDir=$myDir, pos.alignToNorth(myDir)=${vec.alignToNorth(myDir)}, result=${
        moveTo(
            vec.alignToNorth(
                myDir
            )
        )
        }"
    )
    return moveTo(vec.alignFromNorth(myDir))
}

fun moveCloseTo(dim: Dim, vec: Vec): Command {
    val radiusX = dim.width / 2
    val radiusY = dim.height / 2
    return when {
        vec.y < -radiusY -> FORWARD
        vec.y > radiusY -> BACKWARD
        vec.x < 0 -> LEFT
        else -> RIGHT
    }
}

fun moveCloseTo(dim: Dim, vec: Vec, myDir: Dir): Command {
    return moveCloseTo(dim, vec.alignFromNorth(myDir))
}

fun BotMap.dim(): Dim = Dim(this[0].length, this.size)

// We just assume the view is a square
fun BotMap.viewRadius(): Int = this.size / 2

fun BotMap.zipWithVec(): Map<Vec, Char> {
    val dim = dim()
    return this.joinToString("").withIndex().map { Pair(vecFromPlayer(dim, it.index), it.value) }.toMap()
}

fun BotMap.charAt(vec: Vec): Char? {
    val dim = dim()
    return this.getOrNull(dim.height / 2 + vec.y)?.getOrNull(dim.width / 2 + vec.x)
}

fun BotMap.canMoveForward(): Boolean {
    val c = charAt(Vec(0, -1))
    return c != null && c == '.'
}

fun BotMap.canMoveBackward(): Boolean {
    val c = charAt(Vec(0, 1))
    return c != null && c == '.'
}

/**
 * @param dim The dimension of the view area
 * @param index The index of the object to be located
 * @returns The vector from the bots position (center of view area) to the object at [index]
 */
fun vecFromPlayer(dim: Dim, index: Int): Vec {
    require(dim.width % 2 == 1)
    require(dim.height % 2 == 1)
    return Vec(index % dim.width - dim.width / 2, index / dim.width - dim.height / 2)
}

fun findObject(view: BotMap, predicate: (Char) -> Boolean): Vec? {
    val i = view.joinToString("").indexOfFirst(predicate)
    return if (i >= 0) vecFromPlayer(view.dim(), i)
    else null
}

fun findObject(view: BotMap, o: Char): Vec? = findObject(view) { it == o }

fun BotMap.playerSymbol(): Char {
    val dim = dim()
    return this[dim.height / 2][dim.width / 2]
}

interface BreadthFirstResult {
    fun path(target: Vec): List<Node>?
}

data class Node(val pos: Vec, val dir: Dir)

private inline fun canSee(n: Node, v: Vec, viewRadius: Int): Boolean = (v - n.pos).manhattanDistance() <= viewRadius

fun Node.neighbors(): List<Node> = listOf(
    this.copy(dir = dir.left()),
    this.copy(dir = dir.right()),
    this.copy(pos = pos + dir.toVec()),
    this.copy(pos = pos - dir.toVec()),
)

fun command(a: Node, b: Node): Command {
    if (a.pos == b.pos) {
        if (a.dir == b.dir.left()) return '<'
        if (a.dir == b.dir.right()) return '>'
        error("No possible command to move from $a to $b")
    }
    val v = a.dir.toVec()
    if (a.pos == b.pos + v) return '^'
    if (a.pos == b.pos - v) return 'v'
    error("No possible command to move from $a to $b")
}

fun breadthFirst(pos: Vec, dir: Dir, obstacles: Set<Vec>, viewRadius: Int, maxCost: Int = 20): BreadthFirstResult {
    // println("#breadthFirst, pos: $pos, dir: $dir, obstacles: ${obstacles.size}")
    val startNode = Node(pos, dir)
    var frontier = mutableListOf<Node>(startNode)
    val cameFrom = mutableMapOf<Node, Node>(
        startNode to startNode
    )
    val costs = mutableMapOf<Node, Int>(
        startNode to 0
    )

    var currentCost = 0
    while (frontier.isNotEmpty() && currentCost <= maxCost) {
        currentCost++
        val nextFrontier = mutableListOf<Node>()
        for (current in frontier) {
            for (n in current.neighbors()) {
                if (n in cameFrom) continue
                if (n.pos in obstacles) continue
                nextFrontier.add(n)
                cameFrom[n] = current
                costs[n] = currentCost
            }
        }
        frontier = nextFrontier
    }

    fun Node.path(): List<Node> {
        val result = mutableListOf<Node>(this)
        var n = this
        while (true) {
            val n2 = cameFrom[n] ?: error("")
            if (n == n2) return result
            n = n2
            result.add(n2)
        }
    }

    fun targetToNode(target: Vec): Node? {
        val e: Map.Entry<Node, Int>? =
            costs.filter { canSee(it.key, target, viewRadius) }.minByOrNull { entry -> entry.value }
        return e?.key
    }

    return object : BreadthFirstResult {
        override fun path(target: Vec): List<Node>? {
            val n = targetToNode(target)
            return n?.path()
        }
    }
}
