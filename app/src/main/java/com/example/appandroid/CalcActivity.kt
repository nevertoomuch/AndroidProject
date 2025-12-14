package com.example.appandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class CalcActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private var currentInput = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc)

        resultText = findViewById(R.id.resultTextView)

        digitButtons()
        operatorButtons()
        functionButtons()
    }

    private fun digitButtons() {
        val digitButtons = mapOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        for ((id, digit) in digitButtons) {
            findViewById<Button>(id).setOnClickListener {
                currentInput += digit
                resultText.text = currentInput
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener {
            if (!currentInput.contains(".")) {
                currentInput += "."
                resultText.text = currentInput
            }
        }
    }

    private fun operatorButtons() {
        findViewById<Button>(R.id.btnPlus).setOnClickListener { addOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { addOperator("-") }
        findViewById<Button>(R.id.btnYmnz).setOnClickListener { addOperator("*") }
        findViewById<Button>(R.id.btnDelenie).setOnClickListener { addOperator("/") }
    }

    private fun addOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            val lastChar = currentInput.last()
            if (lastChar.isDigit() || lastChar == '.') {
                currentInput += op
                resultText.text = currentInput
            }
        }
    }

    private fun functionButtons() {
        findViewById<Button>(R.id.btnRavno).setOnClickListener {
            if (currentInput.isNotEmpty()) {
                val lastChar = currentInput.last()
                if (lastChar.isDigit() || lastChar == '.') {
                    if (currentInput == "1337") {
                        currentInput = "=) \n ru.wikipedia.org/wiki/Leet"
                        resultText.text = currentInput
                    } else {
                        val operators = listOf("+", "-", "*", "/")
                        if (operators.any { currentInput.contains(it) }) {
                            try {
                                val result = vichislenie(currentInput)
                                currentInput = formatResult(result)
                                resultText.text = currentInput
                            } catch (e: ArithmeticException) {
                                currentInput = "Ошибка!"
                                resultText.text = currentInput
                            }
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            currentInput = ""
            resultText.text = currentInput
        }
    }

    private fun vichislenie(viragenie: String): Double {
        var firstNumber = ""
        var operator = ""
        var secondNumber = ""

        var index = 0
        while (index < viragenie.length && (viragenie[index].isDigit() || viragenie[index] == '.')) {
            firstNumber += viragenie[index]
            index++
        }

        while (index < viragenie.length && !viragenie[index].isDigit() && viragenie[index] != '.') {
            operator += viragenie[index]
            index++
        }

        while (index < viragenie.length) {
            secondNumber += viragenie[index]
            index++
        }

        val num1 = firstNumber.toDouble()
        val num2 = secondNumber.toDouble()

        return when (operator) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> {
                if (num2 == 0.0) throw ArithmeticException("Не определено")
                num1 / num2
            }
            else -> num2
        }
    }

    private fun formatResult(result: Double): String {
        if (result == result.toLong().toDouble()) {
            return result.toLong().toString()
        }
        return result.toString()
    }
}