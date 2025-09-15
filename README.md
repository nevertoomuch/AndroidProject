# AndroidProject

Фадеев Егор ИКС-432

О проекте
В проекте реализована модель случайного блуждания (Random Walk) в двумерном пространстве с координатами (x, y) для группы из 14 человек. Каждый участник за каждый шаг симуляции случайно меняет свои координаты, делая шаг назад, стоя на месте или шаг вперёд.

Класс Human
kotlin
class Human {
    var x = 0
    var y = 0

    fun move() {
        val dx = Random.nextInt(-1, 2)
        val dy = Random.nextInt(-1, 2)

        x += dx
        y += dy 
    }
}
Ход симуляции
Создаётся 14 объектов класса Human с начальными координатами (0,0).

Запрашивается время симуляции.

В цикле от 1 до указанного времени каждый человек выполняет случайное перемещение.

В конце выводятся итоговые координаты каждого человека.

Пример инициализации массива:

kotlin
val humans = arrayOf(
    Human("Виктория", "Багазий", "Викторовна", 432, 20),
    Human("Игнат", "Бенескул", "Максимович", 432, 19),
    // остальные 12 человек
)
Цикл симуляции:

kotlin
var second = 1
while (second <= simulationTiming) {
    println("${second}")

    var i = 0
    while (i < humans.size) {
        humans[i].move()
        i++
    }
    println()
    second++
}

Вывод финального состояния:

kotlin
println("\nКонечное состояние:")
    for (human in humans) {
        println("${human.surname} ${human.name} ${human.second_n}: ${human.getCoordinate()}")
