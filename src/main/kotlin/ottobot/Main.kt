package ottobot

import java.io.BufferedReader
import java.net.Socket

typealias BotMap = List<String>
typealias MoveFun = (BotMap, Int) -> Command

fun runBot(moveFun: MoveFun) {
    println("This is otto-bot")
    val host = System.getenv("HOST") ?: "localhost"
    val port = System.getenv("PORT")?.let(Integer::parseInt) ?: 63187
    println("Connecting to $host:$port")
    val socket = Socket(host, port)
    socket.use { s ->
        val output = s.outputStream
        val input = s.inputStream.bufferedReader()
        var turn = 0
        while (true) {
            val map = readMap(input)
            turn += 1
            if (map == null) {
                println("Turn $turn, Game ended.")
                break
            } else {
                if (turn == 1) {
                    println("I am ${map.playerSymbol()}")
                }
                val command = moveFun.invoke(map, turn)
                println("command is $command")
                output.write(command.toInt())
            }
        }
    }
}

private fun readMap(input: BufferedReader): BotMap? {
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

typealias Command = Char

const val FORWARD = '^'
const val LEFT = '<'
const val RIGHT = '>'
const val BACKWARD = 'v'
