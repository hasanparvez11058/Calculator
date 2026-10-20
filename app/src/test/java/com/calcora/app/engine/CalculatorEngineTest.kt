package com.calcora.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private val engine = CalculatorEngine()
    private fun result(s: String, angle: String = "DEG") = (engine.evaluate(s, angle) as EvalResult.Success).display

    @Test fun decimalArithmetic() { assertEquals("0.3", result("0.1+0.2")) }
    @Test fun precedence() { assertEquals("13", result("7+3*2")); assertEquals("20", result("(7+3)*2")) }
    @Test fun powerIsRightAssociative() { assertEquals("512", result("2^3^2")) }
    @Test fun scientificFunctions() { assertEquals("0.5", result("sin(30)")); assertEquals("120", result("5!")); assertEquals("1", result("ln(e)")) }
    @Test fun divideByZeroHasFriendlyMessage() { val r = engine.evaluate("1/0"); assertTrue(r is EvalResult.Error); assertEquals("Can't divide by zero", (r as EvalResult.Error).message) }
    @Test fun implicitBracketsNotRequiredForBasicMath() { assertEquals("14", result("2*(3+4)")) }
}
