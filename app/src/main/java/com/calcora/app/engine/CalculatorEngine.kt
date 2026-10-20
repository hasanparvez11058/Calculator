package com.calcora.app.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.*

enum class ErrorType { DIVIDE_BY_ZERO, INVALID_INPUT, FORMAT, UNDEFINED }
sealed interface EvalResult {
    data class Success(val value: BigDecimal, val display: String) : EvalResult
    data class Error(val type: ErrorType, val message: String) : EvalResult
}

/**
 * Local expression parser. Basic arithmetic uses BigDecimal throughout, so 0.1 + 0.2
 * is exactly 0.3. Transcendental functions use the JVM math library's Double precision.
 */
class CalculatorEngine(private val decimalPlaces: Int = 10) {
    fun evaluate(expression: String, angleUnit: String = "DEG"): EvalResult = try {
        if (expression.isBlank() || expression.length > 200) throw FormatException()
        val value = Parser(expression, angleUnit).parse()
        val cleaned = value.stripTrailingZeros()
        EvalResult.Success(cleaned, format(cleaned))
    } catch (e: DivideException) {
        EvalResult.Error(ErrorType.DIVIDE_BY_ZERO, "Can't divide by zero")
    } catch (e: InvalidException) {
        EvalResult.Error(ErrorType.INVALID_INPUT, "Invalid input")
    } catch (e: UndefinedException) {
        EvalResult.Error(ErrorType.UNDEFINED, "Undefined")
    } catch (_: Exception) {
        EvalResult.Error(ErrorType.FORMAT, "Format error")
    }

    private fun format(n: BigDecimal): String {
        val rounded = n.setScale(decimalPlaces.coerceIn(0, 16), RoundingMode.HALF_UP).stripTrailingZeros()
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        val df = java.text.DecimalFormat("#,##0.################", symbols)
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(rounded)
    }

    private class DivideException : RuntimeException()
    private class InvalidException : RuntimeException()
    private class UndefinedException : RuntimeException()
    private class FormatException : RuntimeException()

    private data class Value(val number: BigDecimal, val percentLiteral: BigDecimal? = null)
    private val mc = MathContext.DECIMAL128

    private inner class Parser(private val source: String, private val angle: String) {
        private val s = source.replace("×", "*").replace("÷", "/").replace("−", "-").replace("π", "pi")
        private var p = 0
        fun parse(): BigDecimal {
            val value = addSub().number
            skip()
            if (p != s.length) throw FormatException()
            if (value.precision() > 1000 || value.scale().absoluteValue > 1000) throw InvalidException()
            return value
        }
        private fun skip() { while (p < s.length && s[p].isWhitespace()) p++ }
        private fun eat(c: Char): Boolean { skip(); if (p < s.length && s[p] == c) { p++; return true }; return false }
        private fun addSub(): Value {
            var left = mulDiv()
            while (true) {
                val add = when { eat('+') -> true; eat('-') -> false; else -> return left }
                val right = mulDiv()
                val operand = if (right.percentLiteral != null) left.number.multiply(right.percentLiteral, mc).divide(BigDecimal("100"), mc) else right.number
                left = Value(if (add) left.number.add(operand, mc) else left.number.subtract(operand, mc))
            }
        }
        private fun mulDiv(): Value {
            var left = unary()
            while (true) {
                when {
                    eat('*') -> left = Value(left.number.multiply(unary().number, mc))
                    eat('/') -> {
                        val b = unary().number
                        if (b.compareTo(BigDecimal.ZERO) == 0) throw DivideException()
                        left = Value(left.number.divide(b, mc))
                    }
                    else -> return left
                }
            }
        }
        private fun unary(): Value {
            if (eat('-')) { val v = unary(); return Value(v.number.negate(mc), v.percentLiteral?.negate(mc)) }
            if (eat('+')) return unary()
            return power()
        }
        private fun power(): Value {
            var v = postfix()
            if (eat('^')) {
                val exponent = unary().number
                val base = v.number
                val expInt = try { exponent.intValueExact() } catch (_: Exception) { null }
                v = if (expInt != null && expInt in -1000..1000) {
                    if (expInt < 0 && base.compareTo(BigDecimal.ZERO) == 0) throw DivideException()
                    if (expInt >= 0) Value(base.pow(expInt, mc)) else Value(BigDecimal.ONE.divide(base.pow(-expInt, mc), mc))
                } else {
                    val result = base.toDouble().pow(exponent.toDouble())
                    if (!result.isFinite()) throw InvalidException()
                    Value(BigDecimal.valueOf(result))
                }
            }
            return v
        }
        private fun postfix(): Value {
            var v = primary()
            while (true) {
                when {
                    eat('%') -> v = Value(v.number.divide(BigDecimal("100"), mc), v.number)
                    eat('!') -> {
                        val n = try { v.number.intValueExact() } catch (_: Exception) { throw InvalidException() }
                        if (n !in 0..170) throw InvalidException()
                        var result = BigDecimal.ONE
                        for (i in 2..n) result = result.multiply(BigDecimal.valueOf(i.toLong()), mc)
                        v = Value(result)
                    }
                    else -> return v
                }
            }
        }
        private fun primary(): Value {
            skip()
            if (eat('(')) { val v = addSub(); if (!eat(')')) throw FormatException(); return v }
            val start = p
            var dots = 0
            while (p < s.length && (s[p].isDigit() || s[p] == '.')) { if (s[p] == '.') dots++; p++ }
            if (p > start) {
                if (dots > 1) throw FormatException()
                return Value(s.substring(start, p).toBigDecimalOrNull() ?: throw FormatException())
            }
            val nameStart = p
            while (p < s.length && (s[p].isLetter() || s[p] == '²')) p++
            if (p == nameStart) throw FormatException()
            val name = s.substring(nameStart, p).lowercase(Locale.ROOT)
            if (name == "pi") return Value(BigDecimal.valueOf(Math.PI))
            if (name == "e") return Value(BigDecimal.valueOf(Math.E))
            if (name == "rand" || name == "random") return Value(BigDecimal.valueOf(Math.random()))
            val argument = if (eat('(')) {
                val v = addSub().number
                if (!eat(')')) throw FormatException()
                v
            } else if (name == "sqrt" || name == "√") throw FormatException() else throw FormatException()
            val x = argument.toDouble()
            fun fromDouble(d: Double): Value {
                if (!d.isFinite()) throw InvalidException()
                return Value(BigDecimal.valueOf(if (abs(d) < 1e-15) 0.0 else d))
            }
            val unit = angle.uppercase(Locale.ROOT)
            fun radians(v: Double) = when (unit) { "DEG" -> Math.toRadians(v); "GRAD" -> v * Math.PI / 200.0; else -> v }
            fun angleResult(v: Double) = when (unit) { "DEG" -> Math.toDegrees(v); "GRAD" -> v * 200.0 / Math.PI; else -> v }
            return when (name) {
                "sin" -> fromDouble(sin(radians(x)))
                "cos" -> fromDouble(cos(radians(x)))
                "tan" -> { val r = radians(x); if (abs(cos(r)) < 1e-12) throw UndefinedException(); fromDouble(tan(r)) }
                "asin" -> if (x in -1.0..1.0) fromDouble(angleResult(asin(x))) else throw InvalidException()
                "acos" -> if (x in -1.0..1.0) fromDouble(angleResult(acos(x))) else throw InvalidException()
                "atan" -> fromDouble(angleResult(atan(x)))
                "sinh" -> fromDouble(sinh(x)); "cosh" -> fromDouble(cosh(x)); "tanh" -> fromDouble(tanh(x))
                "ln" -> if (x > 0) fromDouble(ln(x)) else throw InvalidException()
                "log" -> if (x > 0) fromDouble(log10(x)) else throw InvalidException()
                "log2" -> if (x > 0) fromDouble(log2(x)) else throw InvalidException()
                "sqrt", "√" -> if (x >= 0) fromDouble(sqrt(x)) else throw InvalidException()
                "abs" -> Value(argument.abs())
                "exp" -> fromDouble(exp(x))
                "recip", "inverse" -> { if (argument.compareTo(BigDecimal.ZERO) == 0) throw DivideException(); Value(BigDecimal.ONE.divide(argument, mc)) }
                "sqr" -> Value(argument.multiply(argument, mc))
                else -> throw FormatException()
            }
        }
    }
}
