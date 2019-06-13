package ottobot

data class Dim(val width: Int, val height: Int)

data class Vec(val x: Int, val y: Int) : Comparable<Vec> {
    override fun compareTo(other: Vec): Int = compareValuesBy(this, other, { it.x }, { it.y })
}

operator fun Vec.plus(v: Vec): Vec = Vec(x + v.x, y + v.y)
operator fun Vec.minus(v: Vec): Vec = Vec(x - v.x, y - v.y)

fun Vec.neighbors(): List<Vec> = listOf(Vec(x + 1, y), Vec(x, y + 1), Vec(x - 1, y), Vec(x, y - 1))

/** @returns the number of moves to reach [this]
 * (when x != 0 we need to turn once) */
fun Vec.movesAway(): Int = y + if (x == 0) 0 else 1 + x

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
    println("moveTo, pos=$vec, myDir=$myDir, pos.alignToNorth(myDir)=${vec.alignToNorth(myDir)}, result=${moveTo(vec.alignToNorth(myDir))}")
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
