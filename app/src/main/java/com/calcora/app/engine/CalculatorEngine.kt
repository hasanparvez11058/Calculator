package com.calcora.app.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

enum class ErrorType { DIVIDE_BY_ZERO, INVALID_INPUT, FORMAT }
sealed interface EvalResult {
    data class Success(val value: BigDecimal, val display: String) : EvalResult
    data class Error(val type: ErrorType, val message: String) : EvalResult
}

/** Decimal-safe recursive-descent calculator engine. Supports basic arithmetic and common scientific functions. */
class CalculatorEngine {
    fun evaluate(expression: String, angleUnit: String = "DEG"): EvalResult = try {
        if (expression.length > 200) throw FormatException()
        val parser = Parser(expression.replace("×", "*").replace("÷", "/").replace("−", "-"), angleUnit)
        val value = parser.parse()
        if (!value.isFinite()) throw InvalidException()
        val decimal = BigDecimal.valueOf(if (abs(value) < 1e-14) 0.0 else value).round(MathContext.DECIMAL128)
        EvalResult.Success(decimal, format(decimal))
    } catch (e: DivideException) {
        EvalResult.Error(ErrorType.DIVIDE_BY_ZERO, "Can't divide by zero")
    } catch (e: InvalidException) {
        EvalResult.Error(ErrorType.INVALID_INPUT, "Invalid input")
    } catch (_: Exception) {
        EvalResult.Error(ErrorType.FORMAT, "Format error")
    }

    private fun format(n: BigDecimal): String {
        val plain = n.stripTrailingZeros().toPlainString()
        val parts = plain.split(".")
        val integer = parts[0]
        val grouped = integer.reversed().chunked(3).joinToString(",").reversed()
        return if (parts.size > 1) "$grouped.${parts[1]}" else grouped
    }

    private class DivideException : RuntimeException()
    private class InvalidException : RuntimeException()
    private class FormatException : RuntimeException()

    private class Parser(private val s: String, private val angle: String) {
        private var p = 0
        fun parse(): Double { val v = addSub(); skip(); if (p != s.length) throw FormatException(); return v }
        private fun skip() { while (p < s.length && s[p].isWhitespace()) p++ }
        private fun eat(c: Char): Boolean { skip(); if (p < s.length && s[p] == c) { p++; return true }; return false }
        private fun addSub(): Double { var v = mulDiv(); while (true) { v = when { eat('+') -> v + mulDiv(); eat('-') -> v - mulDiv(); else -> return v } } }
        private fun mulDiv(): Double {
            var v = power()
            while (true) {
                when {
                    eat('*') -> { val b = power(); v *= if (s.getOrNull(p) == '%') { p++; b / 100.0 } else b }
                    eat('/') -> { val b = power(); if (b == 0.0) throw DivideException(); v /= b }
                    else -> return v
                }
            }
        }
        private fun power(): Double {
            var v = unary()
            if (eat('^')) v = v.pow(power())
            return v
        }
        private fun unary(): Double {
            if (eat('-')) return -unary()
            if (eat('+')) return unary()
            var v = primary()
            while (true) {
                when {
                    eat('%') -> v /= 100.0
                    eat('!') -> { if (v < 0 || v > 170 || v % 1.0 != 0.0) throw InvalidException(); v = (2..v.toInt()).fold(1.0) { a, n -> a * n } }
                    else -> return v
                }
            }
        }
        private fun primary(): Double {
            skip()
            if (eat('(')) { val v = addSub(); if (!eat(')')) throw FormatException(); return v }
            val start = p
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++
            if (p > start) return s.substring(start, p).toDoubleOrNull() ?: throw FormatException()
            val nameStart = p
            while (p < s.length && s[p].isLetter()) p++
            if (p > nameStart) {
                val name = s.substring(nameStart, p).lowercase()
                val x = if (name == "pi") Math.PI else if (name == "e") Math.E else { if (!eat('(')) throw FormatException(); val a = addSub(); if (!eat(')')) throw FormatException(); a }
                return when (name) {
                    "pi" -> Math.PI; "e" -> Math.E
                    "sin" -> sin(toRadians(x)); "cos" -> cos(toRadians(x))
                    "tan" -> { if (angle == "DEG" && abs(x % 180.0) == 90.0) throw InvalidException(); tan(toRadians(x)) }
                    "asin" -> fromRadians(asin(x)); "acos" -> fromRadians(acos(x)); "atan" -> fromRadians(atan(x))
                    "sinh" -> sinh(x); "cosh" -> cosh(x); "tanh" -> tanh(x)
                    "ln" -> if (x > 0) ln(x) else throw InvalidException()
                    "log" -> if (x > 0) log10(x) else throw InvalidException()
                    "log2" -> if (x > 0) log2(x) else throw InvalidException()
                    "sqrt" -> if (x >= 0) sqrt(x) else throw InvalidException()
                    "abs" -> abs(x)
                    else -> throw FormatException()
                }
            }
            throw FormatException()
        }
        private fun toRadians(x: Double) = if (angle == "DEG") Math.toRadians(x) else x
        private fun fromRadians(x: Double) = if (angle == "DEG") Math.toDegrees(x) else x
    }
}
