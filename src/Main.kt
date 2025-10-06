import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

open class Human(
    var FIO: String,
    var age: Int,
    var speed: Int
) {
    protected var x: Double = 0.0
    protected var y: Double = 0.0
    protected val random = Random.Default

    open fun move() {
        val thread = Thread {
            repeat(5) { i ->
                val alpha = 2 * PI * random.nextDouble()
                x += speed * cos(alpha)
                y += speed * sin(alpha)

                log("$FIO (Человек) перемещается в ($x, $y) [секунда ${i + 1}]")
                Thread.sleep(1000)
            }
        }
        thread.start()
    }

    @Synchronized
    protected fun log(msg: String) {
        println(msg)
    }
}

class Driver(
    FIO: String,
    age: Int,
    speed: Int
) : Human(FIO, age, speed) {

    private var rout: Double = 2 * PI * random.nextDouble()

    override fun move() {
        val thread = Thread {
            repeat(5) { i ->
                x += speed * cos(rout)
                y += speed * sin(rout)

                log("$FIO (Водитель) двигается в ($x, $y) [секунда ${i + 1}]")
                Thread.sleep(1000)
            }
        }
        thread.start()
    }
}

fun main() {
    val humans = listOf(
        Human("Фадеев Е.А", 19, 9),
        Human("Андропов Ф.С", 9, 3),
        Human("Топский П.Е", 21, 11),
        Human("Серегеев С.В", 54, 6)
    )
    val driver = Driver("Кириленко А.Г", 25, 30)

    val all = humans + driver

    all.forEach { it.move() }

    Thread.sleep(6000)
}
