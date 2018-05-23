import Command.*
import java.io.BufferedReader
import java.net.Socket

typealias BotMap = List<String>
typealias MoveFun = (BotMap, Int) -> Command

fun runBot(moveFun: MoveFun) {
    println("Hi from otto-bot")
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
                output.write(moveFun.invoke(map, turn).char.toInt())
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

fun moveTowards(vec: Vec): Command {
    return when {
        vec.y < 0 -> FORWARD
        vec.y > 0 -> BACKWARD
        vec.x < 0 -> LEFT
        else -> RIGHT
    }
}

enum class Command {
    FORWARD {
        override val char: Char = '^'
    },
    LEFT {
        override val char: Char = '<'
    },
    RIGHT {
        override val char: Char = '>'
    },
    BACKWARD {
        override val char: Char = 'v'
    };

    abstract val char: Char
}

data class Dim(val width: Int, val height: Int)

data class Vec(val x: Int, val y: Int)

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
