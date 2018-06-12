package hello

import Command
import MoveFun
import dim
import runBot

fun main(args: Array<String>) {
    println("Running mode 'hello'")
    val moveFun: MoveFun = { map, _ ->
        val dim = map.dim()
        val c = map.get(dim.height / 2)[dim.width / 2]
        println("I am $c")
        Command.FORWARD
    }
    runBot(moveFun)
}