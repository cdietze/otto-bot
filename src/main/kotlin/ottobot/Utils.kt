package ottobot

data class Dim(val width: Int, val height: Int)

data class Vec(val x: Int, val y: Int)

operator fun Vec.plus(v: Vec): Vec = Vec(x + v.x, y + v.y)
operator fun Vec.minus(v: Vec): Vec = Vec(x - v.x, y - v.y)

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

fun moveTowards(vec: Vec): Command {
    return when {
        vec.y < 0 -> Command.FORWARD
        vec.y > 0 -> Command.BACKWARD
        vec.x < 0 -> Command.LEFT
        else -> Command.RIGHT
    }
}

fun BotMap.dim(): Dim = Dim(this[0].length, this.size)

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

fun findObject(view: BotMap, o: Char): Vec? = findObject(view, { it == o })

fun BotMap.playerSymbol(): Char {
    val dim = dim()
    return this.get(dim.height / 2)[dim.width / 2]
}
