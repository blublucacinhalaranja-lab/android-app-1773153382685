package com.pacote.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pacote.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var isBanned: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("CalculatorPrefs", Context.MODE_PRIVATE)
        isBanned = sharedPreferences.getBoolean("isBanned", false)

        if (isBanned) {
            showBannedMessage()
        } else {
            setupCalculator()
        }
    }

    private fun setupCalculator() {
        binding.calculateButton.setOnClickListener {
            val expression = binding.expressionEditText.text.toString()
            if (expression == "1 + 1") {
                banUser()
            } else {
                try {
                    val result = evaluateExpression(expression)
                    binding.resultTextView.text = result.toString()
                } catch (e: Exception) {
                    binding.resultTextView.text = "Error"
                }
            }
        }
    }

    private fun evaluateExpression(expression: String): Double {
        // Simplistic evaluation - replace with a proper expression parser for real use
        return try {
            val result = eval(expression)
            result.toDouble()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Expression")
        }
    }

    private fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch: Char = ' '

            fun nextChar() {
                ch = if ((++pos < str.length)) str[pos] else ' '
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' ') nextChar()
                if (ch.code == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw Exception("Unexpected: " + ch)
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar()
                    val s = str.substring(startPos, pos)
                    x = s.toDouble()
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    if (func == "sqrt") x = kotlin.math.sqrt(x)
                    else if (func == "sin") x = kotlin.math.sin(Math.toRadians(x))
                    else if (func == "cos") x = kotlin.math.cos(Math.toRadians(x))
                    else if (func == "tan") x = kotlin.math.tan(Math.toRadians(x))
                    else throw Exception("Unknown function: " + func)
                } else {
                    throw Exception("Unexpected: " + ch)
                }

                if (eat('^'.code)) x = kotlin.math.pow(x, parseFactor()) // exponentiation

                return x
            }
        }.parse()
    }

    private fun banUser() {
        isBanned = true
        with(sharedPreferences.edit()) {
            putBoolean("isBanned", true)
            apply()
        }
        showBannedMessage()
    }

    private fun showBannedMessage() {
        binding.expressionEditText.visibility = android.view.View.GONE
        binding.calculateButton.visibility = android.view.View.GONE
        binding.resultTextView.visibility = android.view.View.GONE
        binding.bannedTextView.visibility = android.view.View.VISIBLE
    }
}