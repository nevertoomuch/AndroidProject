package classes

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random


class Driver(
    FIO: String,
    age: Int,
    override val speed: Int
) : Human(FIO, age, speed), interfaces.Movable {

    private var route: Double = 2 * PI * Random.nextDouble()

    override fun move() {
        val thread = Thread {
            repeat(5) { i ->
                x += speed * cos(route)
                y += speed * sin(route)

                log("$FIO (Водитель) двигается в (${x.format()}, ${y.format()}) [секунда ${i + 1}]")
                Thread.sleep(1000)
            }
        }
        thread.start()
    }

    private fun Double.format() = "%.2f".format(this)
}