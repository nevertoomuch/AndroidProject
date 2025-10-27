package interfaces

interface Movable {
    val speed: Int
    var x: Double
    var y: Double

    fun move()
}