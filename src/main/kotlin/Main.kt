import java.io.BufferedReader
import java.net.Socket

private fun readMap(input: BufferedReader): List<String>? {
    val result = mutableListOf<String>()
    var lines = 0
    while (true) {
        val line = input.readLine() ?: break
        if (lines < 1) {
            lines = line.length
        }
        result.add(line)
        if (--lines < 1) {
            break
        }
    }
    if (result.isEmpty()) return null
    return result
}

fun main(args: Array<String>) {
    println("Hi, this is otto-bot")
    val socket = Socket(
            if (args.size > 0) args[0] else "localhost",
            if (args.size > 1) Integer.parseInt(args[1]) else 63187
    )
    socket.use { s ->
        val output = s.outputStream
        val input = s.inputStream.bufferedReader()
        var state = State()
        while (true) {
            val sector = readMap(input)
            if (sector == null) {
                println("Game ended.")
                break
            } else {
                val response = move(sector, state)
                state = response.second
                output.write(response.first.toInt())
            }
        }
    }
}

fun move(view: List<String>, s: State): Pair<Char, State> {
    val exit = findObject(view, 'O')
    if (exit != null) {
        println("I CAN SEE THE EXIT!")
        return Pair(moveTo(exit), s)
    }
    return if (s.steps == 0) {
        val command = if (s.forwardMode) '<' else '>'
        val newMode = !s.forwardMode
        Pair(command, s.copy(forwardMode = newMode, steps = if (newMode) FORWARD_STEPS else ORTHOGONAL_STEPS))
    } else {
        Pair('^', s.copy(steps = s.steps - 1))
    }
}

fun moveTo(vec: Vec): Char {
    return when {
        vec.y < 0 -> '^'
        vec.y > 0 -> 'v'
        vec.x < 0 -> '<'
        else -> '>'
    }
}

const val FORWARD_STEPS = 17
const val ORTHOGONAL_STEPS = 5

data class State(val forwardMode: Boolean = true, val steps: Int = FORWARD_STEPS)

data class Dim(val width: Int, val height: Int)
data class Vec(val x: Int, val y: Int)

fun List<String>.dim(): Dim = Dim(this[0].length, this.size)

fun vecFromPlayer(dim: Dim, index: Int): Vec {
    require(dim.width % 2 == 1)
    require(dim.height % 2 == 1)
    return Vec(index % dim.width - dim.width / 2, index / dim.width - dim.height / 2)
}

fun findObject(view: List<String>, o: Char): Vec? {
    val i = view.joinToString("").indexOf(o)
    return if (i >= 0) vecFromPlayer(view.dim(), i)
    else null
}
