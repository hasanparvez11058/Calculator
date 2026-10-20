package com.calcora.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {
    private val engine = CalculatorEngine()
    private fun result(s: String, angle: String = "DEG") = (engine.evaluate(s, angle) as EvalResult.Success).display
    private fun error(s: String) = engine.evaluate(s) as EvalResult.Error

    @Test fun decimalArithmeticIsExact() { assertEquals("0.3", result("0.1+0.2")); assertEquals("0.02", result("0.1*0.2")) }
    @Test fun precedenceAndBrackets() { assertEquals("13", result("7+3*2")); assertEquals("20", result("(7+3)*2")) }
    @Test fun powerAndUnaryPrecedence() { assertEquals("512", result("2^3^2")); assertEquals("-4", result("-2^2")); assertEquals("0.25", result("2^-2")) }
    @Test fun scientificFunctions() { assertEquals("0.5", result("sin(30)")); assertEquals("1", result("ln(e)")); assertEquals("120", result("5!")); assertEquals("3", result("sqrt(9)")) }
    @Test fun percentagesFollowCalculatorRules() { assertEquals("220", result("200+10%")); assertEquals("180", result("200-10%")); assertEquals("5", result("50*10%")); assertEquals("0.5", result("50%")) }
    @Test fun divideByZeroHasFriendlyMessage() { assertEquals("Can't divide by zero", error("1/0").message) }
    @Test fun invalidAndUndefinedAreDistinguished() { assertEquals("Invalid input", error("sqrt(-1)").message); assertEquals("Undefined", error("tan(90)").message); assertTrue(engine.evaluate("2+").isError()) }
    @Test fun groupingAndLargeNumbers() { assertEquals("1,000,000", result("1000000")); assertEquals("100,000,000,000,000,000,000", result("100000000000000000000")) }
    @Test fun randomizedDecimalAddition() {
        val random = java.util.Random(42)
        repeat(500) {
            val a = random.nextInt(2_000_001) - 1_000_000
            val b = random.nextInt(2_000_001) - 1_000_000
            val actual = engine.evaluate("$a+$b") as EvalResult.Success
            assertEquals(0, BigDecimal.valueOf(a.toLong() + b.toLong()).compareTo(actual.value))
        }
    }
    private fun EvalResult.isError() = this is EvalResult.Error
}
