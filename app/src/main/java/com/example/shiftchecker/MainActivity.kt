package com.example.shiftchecker

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shiftchecker.ui.theme.ShiftCheckerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShiftCheckerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShiftCheckerScreen()
                }
            }
        }
    }
}

data class ShiftType(val name: String, val workDays: Int, val restDays: Int)

val shiftTypes = listOf(
    ShiftType("上一休二", 1, 2),
    ShiftType("上一休三", 1, 3),
    ShiftType("上一休四", 1, 4)
)

private const val PREFS_NAME = "shift_prefs"
private const val KEY_BASE_DATE = "base_date"

fun saveBaseDate(context: Context, date: LocalDate) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_BASE_DATE, date.toString())
        .apply()
}

fun loadBaseDate(context: Context): LocalDate {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_BASE_DATE, null)
    return saved?.let { LocalDate.parse(it) } ?: LocalDate.now()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftCheckerScreen() {
    val context = LocalContext.current
    var selectedShift by remember { mutableStateOf(shiftTypes[0]) }
    var baseDate by remember { mutableStateOf(loadBaseDate(context)) }
    var checkDate by remember { mutableStateOf(LocalDate.now()) }
    var showShiftDropdown by remember { mutableStateOf(false) }
    var showBaseDatePicker by remember { mutableStateOf(false) }
    var showCheckDatePicker by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

    fun isWorkingDay(shift: ShiftType, base: LocalDate, check: LocalDate): Boolean {
        if (check.isBefore(base)) {
            val daysBetween = ChronoUnit.DAYS.between(check, base).toInt()
            val cycle = shift.workDays + shift.restDays
            val dayInCycle = ((daysBetween - 1) % cycle + cycle) % cycle
            return dayInCycle < shift.workDays
        } else {
            val daysBetween = ChronoUnit.DAYS.between(base, check).toInt()
            val cycle = shift.workDays + shift.restDays
            return (daysBetween % cycle) < shift.workDays
        }
    }

    val result = isWorkingDay(selectedShift, baseDate, checkDate)

    fun findNextWorkDay(shift: ShiftType, base: LocalDate, from: LocalDate): LocalDate {
        var d = from.plusDays(1)
        while (!isWorkingDay(shift, base, d)) {
            d = d.plusDays(1)
        }
        return d
    }

    val nextWorkDay = findNextWorkDay(selectedShift, baseDate, checkDate)

    if (showBaseDatePicker) {
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    baseDate = LocalDate.of(year, month + 1, dayOfMonth)
                    saveBaseDate(context, baseDate)
                    showBaseDatePicker = false
                },
                baseDate.year,
                baseDate.monthValue - 1,
                baseDate.dayOfMonth
            )
            dialog.setOnDismissListener { showBaseDatePicker = false }
            dialog.show()
            onDispose { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    if (showCheckDatePicker) {
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    checkDate = LocalDate.of(year, month + 1, dayOfMonth)
                    showCheckDatePicker = false
                },
                checkDate.year,
                checkDate.monthValue - 1,
                checkDate.dayOfMonth
            )
            dialog.setOnDismissListener { showCheckDatePicker = false }
            dialog.show()
            onDispose { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "轮班查询",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        ExposedDropdownMenuBox(
            expanded = showShiftDropdown,
            onExpandedChange = { showShiftDropdown = it }
        ) {
            OutlinedTextField(
                value = selectedShift.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("值班类型") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showShiftDropdown) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = showShiftDropdown,
                onDismissRequest = { showShiftDropdown = false }
            ) {
                shiftTypes.forEach { shift ->
                    DropdownMenuItem(
                        text = { Text(shift.name) },
                        onClick = {
                            selectedShift = shift
                            showShiftDropdown = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBaseDatePicker = true }
        ) {
            OutlinedTextField(
                value = baseDate.format(formatter),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("基准日期（起始上班日）") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCheckDatePicker = true }
        ) {
            OutlinedTextField(
                value = checkDate.format(formatter),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("选择日期") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (result)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (result) "🔴 上班" else "🟢 休息",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (result)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (!result) {
                    Text(
                        text = "下一个值班日期为${nextWorkDay.format(formatter)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Text(
            text = "计算规则：${selectedShift.name} = 上${selectedShift.workDays}天，休${selectedShift.restDays}天\n以基准日期为第1个上班日进行循环",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
