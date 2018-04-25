import java.io.BufferedReader
import java.net.Socket

typealias Sector = List<String>

private fun readMap(input: BufferedReader): Sector? {
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
        while (true) {
            val sector = readMap(input)
            if (sector == null) {
                println("Game ended.")
                break
            } else {
                val command = move(sector)
                output.write(command.toInt())
            }
        }
    }
}

fun move(sector: List<String>): Char = 'v'
