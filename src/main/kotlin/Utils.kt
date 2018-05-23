data class Dim(val width: Int, val height: Int)

data class Vec(val x: Int, val y: Int)

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

fun findObject(view: BotMap, o: Char): Vec? {
    val i = view.joinToString("").indexOf(o)
    return if (i >= 0) vecFromPlayer(view.dim(), i)
    else null
}
