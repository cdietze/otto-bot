object Mission0 {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hi from ${this.javaClass}")
        val moveFun: MoveFun = { map, _ ->
            val dim = map.dim()
            val c = map.get(dim.height / 2)[dim.width / 2]
            println("I am $c")
            Command.FORWARD
        }
        runBot(moveFun)
    }
}
