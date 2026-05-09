package com.example.shiftchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftCheckerScreen() {
    var selectedShift by remember { mutableStateOf(shiftTypes[0]) }
    var baseDate by remember { mutableStateOf(LocalDate.now()) }
    var checkDate by remember { mutableStateOf(LocalDate.now()) }
    var showShiftDropdown by remember { mutableStateOf(false) }
    var showBaseDatePicker by remember { mutableStateOf(false) }
    var showCheckDatePicker by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

    fun isWorkingDay(shift: ShiftType, base: LocalDate, check: LocalDate): Boolean {
        if (check.isBefore(base)) {
            // 基准日期之前，倒推
            val daysBetween = ChronoUnit.DAYS.between(check, base).toInt()
            val cycle = shift.workDays + shift.restDays
            val dayInCycle = ((daysBetween - 1) % cycle + cycle) % cycle
            return dayInCycle < shift.workDays
        } else {
            // 基准日期当天或之后
            val daysBetween = ChronoUnit.DAYS.between(base, check).toInt()
            val cycle = shift.workDays + shift.restDays
            return (daysBetween % cycle) < shift.workDays
        }
    }

    val result = isWorkingDay(selectedShift, baseDate, checkDate)

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

        // 值班类型选择
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

        // 基准日期选择
        OutlinedTextField(
            value = baseDate.format(formatter),
            onValueChange = {},
            readOnly = true,
            label = { Text("基准日期（起始上班日）") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showBaseDatePicker = true }
        )

        // 查询日期选择
        OutlinedTextField(
            value = checkDate.format(formatter),
            onValueChange = {},
            readOnly = true,
            label = { Text("选择日期") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCheckDatePicker = true }
        )

        // 结果显示卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (result)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (result) "🔴 上班" else "🟢 休息",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (result)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 说明文字
        Text(
            text = "计算规则：${selectedShift.name} = 上${selectedShift.workDays}天，休${selectedShift.restDays}天\n以基准日期为第1个上班日进行循环",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }

    // 基准日期选择器
    if (showBaseDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showBaseDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showBaseDatePicker = false }) {
                    Text("确定")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = baseDate.toEpochDay() * 86400000
            )
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let {
                    baseDate = LocalDate.ofEpochDay(it / 86400000)
                }
            }
        }
    }

    // 查询日期选择器
    if (showCheckDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCheckDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showCheckDatePicker = false }) {
                    Text("确定")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = checkDate.toEpochDay() * 86400000
            )
            DatePicker(state = datePickerState)
            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let {
                    checkDate = LocalDate.ofEpochDay(it / 86400000)
                }
            }
        }
    }
}
