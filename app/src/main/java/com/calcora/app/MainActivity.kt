package com.calcora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calcora.app.engine.CalculatorEngine
import com.calcora.app.engine.EvalResult
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val Emerald = Color(0xFF006B5E)
private val Mint = Color(0xFFCCE8E1)
private val Warm = Color(0xFFFFDCBD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalcoraApp() }
    }
}

@Composable
private fun CalcoraApp() {
    var dark by remember { mutableStateOf(false) }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = Color(0xFF82D5C3), secondaryContainer = Color(0xFF334B46), background = Color(0xFF0E1513), surface = Color(0xFF1A211F))
        else lightColorScheme(primary = Emerald, secondaryContainer = Mint, background = Color(0xFFF4FBF8), surface = Color(0xFFE8EFEC), tertiaryContainer = Warm)
    ) {
        CalculatorScreen(dark = dark, onDarkChange = { dark = it })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var scientific by remember { mutableStateOf(false) }
    var angle by remember { mutableStateOf("DEG") }
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    val engine = remember { CalculatorEngine() }
    val preview = remember(expression, angle) {
        if (expression.isBlank()) null else (engine.evaluate(expression, angle) as? EvalResult.Success)?.display
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.History, contentDescription = "Calculation history") }
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(selected = !scientific, onClick = { scientific = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Standard") }
                SegmentedButton(selected = scientific, onClick = { scientific = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Scientific") }
            }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 4.dp)) {
            Column(Modifier.fillMaxWidth().weight(0.9f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).padding(18.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                BasicTextField(value = expression, onValueChange = { expression = it.take(200); error = null }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), decorationBox = { inner -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { if (expression.isEmpty()) Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp); inner() } })
                Spacer(Modifier.height(8.dp))
                AnimatedContent(targetState = preview ?: result, label = "result") { value ->
                    Text(value, fontSize = 48.sp, lineHeight = 54.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, maxLines = 1)
                }
                if (preview != null) Text("Live result", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            if (scientific) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("sin(", "cos(", "tan(", "ln(", "log(", "√(", "x²", "xʸ", "π", "e", "n!", "1/x", "2nd", angle).forEach { key ->
                        FilledTonalButton(onClick = { when (key) { "2nd" -> {}; "DEG" -> angle = "RAD"; "RAD" -> angle = "DEG"; else -> expression += when(key) { "π" -> "pi"; "x²" -> "^2"; "xʸ" -> "^"; "n!" -> "!"; "1/x" -> "1/("; else -> key } } }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f)) { Text(key, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            val rows = listOf(
                listOf("C", "( )", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("+/−", "0", ".", "=")
            )
            Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "−", "+", "%", "( )")
                            val isSpecial = key == "C"
                            val isEquals = key == "="
                            val bg = when { isEquals -> MaterialTheme.colorScheme.primary; isSpecial -> MaterialTheme.colorScheme.tertiaryContainer; isOperator -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surface }
                            val fg = when { isEquals -> MaterialTheme.colorScheme.onPrimary; isSpecial -> MaterialTheme.colorScheme.onTertiaryContainer; isOperator -> MaterialTheme.colorScheme.onSecondaryContainer; else -> MaterialTheme.colorScheme.onSurface }
                            Surface(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                when (key) {
                                    "C" -> { expression = ""; result = "0"; error = null }
                                    "=" -> {
                                        when (val evaluated = engine.evaluate(expression, angle)) {
                                            is EvalResult.Success -> { result = evaluated.display; history.add(0, expression to result); expression = result; error = null }
                                            is EvalResult.Error -> error = evaluated.message
                                        }
                                    }
                                    "+/−" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
                                    "×" -> expression += "*"
                                    "÷" -> expression += "/"
                                    "−" -> expression += "-"
                                    "( )" -> expression += if (expression.count { it == '(' } > expression.count { it == ')' }) ")" else "("
                                    "⌫" -> expression = expression.dropLast(1)
                                    else -> { expression += key; error = null }
                                }
                            }, shape = RoundedCornerShape(24.dp), color = bg, contentColor = fg, tonalElevation = if (key.all { it.isDigit() }) 1.dp else 0.dp) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "C") Text("C", fontSize = 23.sp, fontWeight = FontWeight.Medium)
                                    else Text(key, fontSize = 25.sp, fontWeight = if (isEquals) FontWeight.SemiBold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistory) AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("History") }, text = {
        if (history.isEmpty()) Text("No calculations yet") else LazyColumn { items(history) { item -> Column(Modifier.fillMaxWidth().clickable { expression = item.first; result = item.second; showHistory = false }.padding(vertical = 8.dp)) { Text(item.first, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.second, fontSize = 22.sp) }; HorizontalDivider() } }
    }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } }, dismissButton = { TextButton(onClick = { history.clear() }) { Text("Clear all") } })
    if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Settings") }, text = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Dark theme"); Switch(checked = dark, onCheckedChange = onDarkChange) }
        Text("Calcora 1.0.0 · Offline and ad-free", style = MaterialTheme.typography.bodySmall)
    }, confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } })
}
