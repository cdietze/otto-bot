package ottobot.word

import ottobot.Dir
import ottobot.Vec

fun main() {
    test1()
    test2()
    test3()
}

fun test1() {
    val result = shortestPathToView(Vec(0, 0), Dir.EAST, listOf(Vec(5, 0)), setOf(), 1)
    check(result!!.size == 4)
    check(result.all { it == '^' })
}

fun test2() {
    val result = shortestPathToView(Vec(0, 0), Dir.EAST, listOf(Vec(5, 0), Vec(-4, 0)), setOf(), 1)
    check(result!!.size == 3)
    check(result.all { it == 'v' })
}

fun test3() {
    val result = shortestPathToView(Vec(0, 0), Dir.EAST, listOf(Vec(4, 0), Vec(-5, 0)), setOf(), 1)
    check(result!!.size == 3)
    check(result.all { it == '^' })
}
