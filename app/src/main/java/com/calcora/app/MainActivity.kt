package com.calcora.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcora.app.engine.CalculatorEngine
import com.calcora.app.engine.EvalResult
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.*

private val Emerald = Color(0xFF006B5E)
private val Mint = Color(0xFFCCE8E1)
private val Warm = Color(0xFFFFDCBD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalcoraApp() }
    }
}

private data class HistoryItem(val expression: String, val result: String, val time: Long, val pinned: Boolean = false)

@Composable
private fun CalcoraApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calcora", Context.MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var amoled by remember { mutableStateOf(prefs.getBoolean("amoled", false)) }
    var palette by remember { mutableStateOf(prefs.getString("palette", "Emerald") ?: "Emerald") }
    val paletteColor = when (palette) {
        "Sunset" -> Color(0xFF984000); "Violet" -> Color(0xFF6750A4); "Rose" -> Color(0xFF9B405A); else -> Emerald
    }
    val scheme = when {
        amoled -> darkColorScheme(primary = Color(0xFF82D5C3), secondaryContainer = Color(0xFF263C37), background = Color.Black, surface = Color(0xFF101010))
        dark -> darkColorScheme(primary = Color(0xFF82D5C3), secondaryContainer = Color(0xFF334B46), background = Color(0xFF0E1513), surface = Color(0xFF1A211F))
        else -> lightColorScheme(primary = paletteColor, secondaryContainer = Mint, background = Color(0xFFF4FBF8), surface = Color(0xFFE8EFEC), tertiaryContainer = Warm)
    }
    MaterialTheme(colorScheme = scheme) {
        CalculatorAppScreen(
            prefs = prefs,
            dark = dark,
            amoled = amoled,
            palette = palette,
            onDark = { dark = it; amoled = false; prefs.edit().putBoolean("dark", it).putBoolean("amoled", false).apply() },
            onAmoled = { amoled = it; dark = it; prefs.edit().putBoolean("amoled", it).putBoolean("dark", it).apply() },
            onPalette = { palette = it; prefs.edit().putString("palette", it).apply() }
        )
    }
}

@Composable
private fun CalculatorAppScreen(
    prefs: android.content.SharedPreferences,
    dark: Boolean,
    amoled: Boolean,
    palette: String,
    onDark: (Boolean) -> Unit,
    onAmoled: (Boolean) -> Unit,
    onPalette: (String) -> Unit
) {
    val context = LocalContext.current
    val engine = remember { CalculatorEngine() }
    var expression by remember { mutableStateOf(prefs.getString("expression", "") ?: "") }
    var result by remember { mutableStateOf(prefs.getString("result", "0") ?: "0") }
    var scientific by remember { mutableStateOf(false) }
    var angle by remember { mutableStateOf(prefs.getString("angle", "DEG") ?: "DEG") }
    var memory by remember { mutableStateOf(prefs.getString("memory", "0") ?: "0") }
    var error by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Calculator") }
    var historySearch by remember { mutableStateOf("") }
    var history by remember {
        val restored = mutableListOf<HistoryItem>()
        try {
            val arr = JSONArray(prefs.getString("history", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                restored.add(HistoryItem(o.getString("expression"), o.getString("result"), o.optLong("time"), o.optBoolean("pinned")))
            }
        } catch (_: Exception) { }
        mutableStateOf(restored)
    }
    fun saveHistory() {
        val arr = JSONArray()
        history.take(1000).forEach { h -> arr.put(JSONObject().put("expression", h.expression).put("result", h.result).put("time", h.time).put("pinned", h.pinned)) }
        prefs.edit().putString("history", arr.toString()).apply()
    }
    fun saveState() { prefs.edit().putString("expression", expression).putString("result", result).putString("angle", angle).putString("memory", memory).apply() }
    val preview = remember(expression, angle) {
        if (expression.isBlank()) null else (engine.evaluate(expression, angle) as? EvalResult.Success)?.display
    }
    fun put(text: String) { expression += text; error = null; saveState() }
    fun press(key: String) {
        try { (context as? android.app.Activity)?.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) { }
        when (key) {
            "C" -> { expression = ""; result = "0"; error = null }
            "⌫" -> { expression = expression.dropLast(1); error = null }
            "=" -> when (val e = engine.evaluate(expression, angle)) {
                is EvalResult.Success -> {
                    val original = expression
                    result = e.display
                    if (original.isNotBlank()) { history.add(0, HistoryItem(original, result, System.currentTimeMillis())); history = history.take(1000).toMutableList(); saveHistory() }
                    expression = result; error = null
                }
                is EvalResult.Error -> error = e.message
            }
            "+/−" -> {
                val v = engine.evaluate(expression, angle)
                expression = when (v) { is EvalResult.Success -> v.value.negate().stripTrailingZeros().toPlainString(); else -> if (expression.startsWith("-")) expression.drop(1) else "-$expression" }
            }
            "( )" -> expression += if (expression.count { it == '(' } > expression.count { it == ')' }) ")" else "("
            "×" -> appendOperator("*")
            "÷" -> appendOperator("/")
            "−" -> appendOperator("-")
            "+", "%" -> if (key == "%") expression += "%" else appendOperator("+")
            else -> {
                if (expression == result && result != "0" && key.firstOrNull()?.isDigit() == true) expression = ""
                expression += when (key) { "π" -> "pi"; "√" -> "sqrt("; "x²" -> "^2"; "xʸ" -> "^"; "n!" -> "!"; "1/x" -> "recip("; "|x|" -> "abs("; "EXP" -> "e^"; "rand" -> "random"; else -> key }
                error = null
            }
        }
        saveState()
    }
    fun appendOperator(op: String) {
        if (expression.isBlank()) { if (op == "-") expression = "-"; return }
        val last = expression.last()
        if (last in "+-*/^") {
            if (op == "-" && last in "*/^") expression += op else expression = expression.dropLast(1) + op
        } else expression += op
        error = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.History, contentDescription = "Calculation history") }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(selected = page == "Calculator", onClick = { page = "Calculator" }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Calc") }
                    SegmentedButton(selected = page == "Converter", onClick = { page = "Converter" }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Convert") }
                    SegmentedButton(selected = page == "Finance", onClick = { page = "Finance" }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("Finance") }
                }
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            }
        }
    ) { padding ->
        when (page) {
            "Converter" -> ConverterScreen(Modifier.fillMaxSize().padding(padding))
            "Finance" -> FinanceScreen(Modifier.fillMaxSize().padding(padding))
            else -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 4.dp)) {
                Column(Modifier.fillMaxWidth().weight(if (scientific) 0.8f else 1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).padding(18.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        if (memory != "0") AssistChip(onClick = { put(memory) }, label = { Text("M") }) else Spacer(Modifier.width(8.dp))
                        Row {
                            IconButton(onClick = { val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("Calcora result", result)) }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy result") }
                            IconButton(onClick = { press("⌫") }) { Icon(Icons.Default.Backspace, contentDescription = "Backspace") }
                        }
                    }
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                    BasicTextField(value = expression, onValueChange = { expression = it.take(200); error = null; saveState() }, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), decorationBox = { inner -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { if (expression.isEmpty()) Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp); inner() } })
                    Spacer(Modifier.height(8.dp))
                    AnimatedContent(targetState = preview ?: result, label = "result") { value ->
                        Text(value, fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, maxLines = 1)
                    }
                    if (preview != null) Text("Live result", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { scientific = !scientific }) { Text(if (scientific) "Hide scientific" else "Scientific") }
                    TextButton(onClick = { angle = when (angle) { "DEG" -> "RAD"; "RAD" -> "GRAD"; else -> "DEG" }; prefs.edit().putString("angle", angle).apply() }) { Text(angle) }
                }
                if (scientific) {
                    Column(Modifier.fillMaxWidth().weight(0.85f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(listOf("sin(", "cos(", "tan(", "ln(", "log("), listOf("√", "x²", "xʸ", "1/x", "n!"), listOf("asin(", "acos(", "atan(", "π", "e"), listOf("sinh(", "cosh(", "tanh(", "|x|", "rand")).forEach { row ->
                            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                row.forEach { key -> FilledTonalButton(onClick = { if (key == "sin(" || key == "cos(" || key == "tan(" || key == "ln(" || key == "log(" || key.endsWith("(")) put(key) else press(key) }, modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(horizontal = 2.dp)) { Text(key, fontSize = 12.sp, maxLines = 1) } }
                            }
                        })
                    }
                }
                val rows = listOf(listOf("C", "( )", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("+/−", "0", ".", "="))
                Column(Modifier.weight(if (scientific) 1.5f else 2f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { key ->
                                val isOperator = key in listOf("÷", "×", "−", "+", "%", "( )")
                                val bg = when { key == "=" -> MaterialTheme.colorScheme.primary; key == "C" -> MaterialTheme.colorScheme.tertiaryContainer; isOperator -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surface }
                                val fg = when { key == "=" -> MaterialTheme.colorScheme.onPrimary; key == "C" -> MaterialTheme.colorScheme.onTertiaryContainer; isOperator -> MaterialTheme.colorScheme.onSecondaryContainer; else -> MaterialTheme.colorScheme.onSurface }
                                Surface(Modifier.weight(1f).fillMaxHeight().clickable { press(key) }, shape = RoundedCornerShape(22.dp), color = bg, contentColor = fg, tonalElevation = 1.dp) {
                                    Box(contentAlignment = Alignment.Center) { Text(key, fontSize = 24.sp, fontWeight = if (key == "=") FontWeight.SemiBold else FontWeight.Medium) }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { memory = "0"; prefs.edit().putString("memory", memory).apply() }) { Text("MC") }
                    TextButton(onClick = { put(memory) }) { Text("MR") }
                    TextButton(onClick = { val v = (engine.evaluate(expression.ifBlank { result }, angle) as? EvalResult.Success)?.value ?: BigDecimal.ZERO; memory = (BigDecimal(memory).add(v)).stripTrailingZeros().toPlainString(); prefs.edit().putString("memory", memory).apply() }) { Text("M+") }
                    TextButton(onClick = { val v = (engine.evaluate(expression.ifBlank { result }, angle) as? EvalResult.Success)?.value ?: BigDecimal.ZERO; memory = (BigDecimal(memory).subtract(v)).stripTrailingZeros().toPlainString(); prefs.edit().putString("memory", memory).apply() }) { Text("M−") }
                    TextButton(onClick = { showHelp = true }) { Text("Help") }
                }
            }
        }
    }

    if (showHistory) AlertDialog(
        onDismissRequest = { showHistory = false },
        title = { Text("History") },
        text = {
            Column {
                OutlinedTextField(value = historySearch, onValueChange = { historySearch = it }, label = { Text("Search history") }, singleLine = true)
                if (history.isEmpty()) Text("No calculations yet", Modifier.padding(12.dp))
                else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(history.filter { it.expression.contains(historySearch, true) || it.result.contains(historySearch, true) }) { item ->
                        Column(Modifier.fillMaxWidth().clickable { expression = item.expression; showHistory = false; saveState() }.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(item.expression, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.result, fontSize = 22.sp) }
                                TextButton(onClick = {
                                    history = history.map { if (it == item) it.copy(pinned = !it.pinned) else it }.toMutableList(); saveHistory()
                                }) { Text(if (item.pinned) "★" else "☆") }
                                TextButton(onClick = { history = history.filterNot { it == item || (it.expression == item.expression && it.time == item.time) }.toMutableList(); saveHistory() }) { Text("Delete") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } },
        dismissButton = { TextButton(onClick = {
            history = history.filter { it.pinned }.toMutableList(); saveHistory()
        }) { Text("Clear unpinned") } }
    )
    if (showSettings) AlertDialog(
        onDismissRequest = { showSettings = false },
        title = { Text("Settings & themes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Dark theme"); Switch(checked = dark && !amoled, onCheckedChange = onDark) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("AMOLED black"); Switch(checked = amoled, onCheckedChange = onAmoled) }
                Text("Color palette")
                listOf("Emerald", "Sunset", "Violet", "Rose").forEach { p -> FilterChip(selected = palette == p, onClick = { onPalette(p) }, label = { Text(p) }) }
                Text("Calcora · Offline · No ads · No tracking", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } }
    )
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Calculator help") }, text = {
        Text("Percent: 200+10% = 220; 200−10% = 180; 50×10% = 5; standalone 50% = 0.5. Use parentheses to control operation order. Scientific angles can be DEG, RAD, or GRAD. Calculations and saved history stay on this device.")
    }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } })
}

@Composable
private fun ConverterScreen(modifier: Modifier = Modifier) {
    var category by remember { mutableStateOf("Length") }
    var from by remember { mutableStateOf("Meter") }
    var to by remember { mutableStateOf("Kilometer") }
    var input by remember { mutableStateOf("1") }
    val units = when (category) {
        "Length" -> listOf("Meter", "Kilometer", "Centimeter", "Millimeter", "Mile", "Yard", "Foot", "Inch")
        "Weight" -> listOf("Kilogram", "Gram", "Milligram", "Pound", "Ounce", "Metric ton")
        "Temperature" -> listOf("Celsius", "Fahrenheit", "Kelvin")
        "Area" -> listOf("Square meter", "Square kilometer", "Hectare", "Acre", "Square foot")
        "Volume" -> listOf("Liter", "Milliliter", "Cubic meter", "US gallon", "Cup")
        "Speed" -> listOf("m/s", "km/h", "mph", "Knot")
        "Time" -> listOf("Second", "Minute", "Hour", "Day", "Week")
        else -> listOf("Byte", "Kilobyte", "Megabyte", "Gigabyte", "Terabyte", "Bit")
    }
    LaunchedEffect(category) { from = units.first(); to = units.getOrElse(1) { units.first() } }
    val converted = remember(input, from, to, category) { convertUnits(input.toDoubleOrNull(), from, to, category) }
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Unit converter", style = MaterialTheme.typography.headlineMedium)
        Text("Works offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Length", "Weight", "Temperature", "Area", "Volume", "Speed", "Time", "Data").forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) } }
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        UnitSelector("From", from, units) { from = it }
        Button(onClick = { val tmp = from; from = to; to = tmp }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("⇅ Swap units") }
        UnitSelector("To", to, units) { to = it }
        Text(converted?.let { formatNumber(it) } ?: "Enter a valid number", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun UnitSelector(label: String, selected: String, units: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected", modifier = Modifier.weight(1f), textAlign = TextAlign.Start); Text("⌄") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { u -> DropdownMenuItem(text = { Text(u) }, onClick = { onSelect(u); expanded = false }) }
        }
    }
}

private fun convertUnits(value: Double?, from: String, to: String, category: String): Double? {
    if (value == null || !value.isFinite()) return null
    if (from == to) return value
    if (category == "Temperature") {
        val c = when (from) { "Celsius" -> value; "Fahrenheit" -> (value - 32) * 5 / 9; else -> value - 273.15 }
        return when (to) { "Celsius" -> c; "Fahrenheit" -> c * 9 / 5 + 32; else -> c + 273.15 }
    }
    val factors = when (category) {
        "Length" -> mapOf("Meter" to 1.0, "Kilometer" to 1000.0, "Centimeter" to .01, "Millimeter" to .001, "Mile" to 1609.344, "Yard" to .9144, "Foot" to .3048, "Inch" to .0254)
        "Weight" -> mapOf("Kilogram" to 1.0, "Gram" to .001, "Milligram" to .000001, "Pound" to .45359237, "Ounce" to .028349523125, "Metric ton" to 1000.0)
        "Area" -> mapOf("Square meter" to 1.0, "Square kilometer" to 1_000_000.0, "Hectare" to 10_000.0, "Acre" to 4046.8564224, "Square foot" to .09290304)
        "Volume" -> mapOf("Liter" to 1.0, "Milliliter" to .001, "Cubic meter" to 1000.0, "US gallon" to 3.785411784, "Cup" to .2365882365)
        "Speed" -> mapOf("m/s" to 1.0, "km/h" to 1 / 3.6, "mph" to .44704, "Knot" to .514444)
        "Time" -> mapOf("Second" to 1.0, "Minute" to 60.0, "Hour" to 3600.0, "Day" to 86400.0, "Week" to 604800.0)
        else -> mapOf("Byte" to 1.0, "Kilobyte" to 1000.0, "Megabyte" to 1_000_000.0, "Gigabyte" to 1_000_000_000.0, "Terabyte" to 1_000_000_000_000.0, "Bit" to .125)
    }
    val f = factors[from] ?: return null
    val t = factors[to] ?: return null
    return value * f / t
}

@Composable
private fun FinanceScreen(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf("Tip") }
    var bill by remember { mutableStateOf("100") }
    var rate by remember { mutableStateOf("10") }
    var people by remember { mutableStateOf("2") }
    val amount = bill.toDoubleOrNull()
    val percent = rate.toDoubleOrNull()
    val count = (people.toIntOrNull() ?: 1).coerceAtLeast(1)
    val valid = amount != null && amount >= 0 && percent != null && percent >= 0
    val extra = if (valid) amount!! * percent!! / 100.0 else 0.0
    val total = when (mode) { "Discount" -> if (valid) amount!! - extra else 0.0; else -> if (valid) amount!! + extra else 0.0 }
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Finance helpers", style = MaterialTheme.typography.headlineMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Tip", "Discount", "Tax").forEachIndexed { index, item -> SegmentedButton(selected = mode == item, onClick = { mode = item }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(item) } }
        }
        OutlinedTextField(value = bill, onValueChange = { bill = it }, label = { Text(if (mode == "Tip") "Bill amount" else "Original amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text(if (mode == "Tip") "Tip %" else if (mode == "Discount") "Discount %" else "Tax/VAT/GST %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        if (mode == "Tip") OutlinedTextField(value = people, onValueChange = { people = it.filter(Char::isDigit).take(4) }, label = { Text("People splitting bill") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        FinanceLine("Base amount", amount?.let(::formatNumber) ?: "—")
        FinanceLine(if (mode == "Discount") "Savings" else if (mode == "Tip") "Tip amount" else "Tax amount", if (valid) formatNumber(extra) else "—")
        FinanceLine(if (mode == "Discount") "Final price" else "Total", if (valid) formatNumber(total) else "—", true)
        if (mode == "Tip") FinanceLine("Per person", if (valid) formatNumber(total / count) else "—", true)
    }
}

@Composable
private fun FinanceLine(label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "—"
    return java.text.DecimalFormat("#,##0.##########", java.text.DecimalFormatSymbols(Locale.US)).format(BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros())
}
