package ottobot.avoid

import ottobot.Command.FORWARD
import ottobot.MoveFun
import ottobot.runBot

fun main(args: Array<String>) {
    println("Running mode 'avoid'")
    val moveFun: MoveFun = { map, turn ->
        // Always running forward works in 1/4 of the cases
        FORWARD
    }
    runBot(moveFun)
}
