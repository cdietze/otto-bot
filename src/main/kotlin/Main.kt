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
//    println("Map: \n${view.joinToString("\n")}")
    if (view.any { it.any { x -> x == 'O' } }) {
        println("I CAN SEE THE EXIT!")
//        throw RuntimeException("I CAN SEE THE EXIT!")
    }
    return if (s.steps == 0) {
        val command = if (s.forwardMode) '<' else '>'
        val newMode = !s.forwardMode
        Pair(command, s.copy(forwardMode = newMode, steps = if (newMode) FORWARD_STEPS else ORTHOGONAL_STEPS))
    } else {
        Pair('^', s.copy(steps = s.steps - 1))
    }
}

const val FORWARD_STEPS = 17
const val ORTHOGONAL_STEPS = 5

data class State(val forwardMode: Boolean = true, val steps: Int = FORWARD_STEPS)