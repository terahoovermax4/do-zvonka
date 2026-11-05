package ru.dozvonka.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dozvonka.app.core.shortRu
import ru.dozvonka.app.data.Schedule
import java.time.DayOfWeek
import java.time.LocalDate

private val schoolDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
fun ScheduleScreen(schedule: Schedule) {
    val c = LocalAppColors.current
    val today = LocalDate.now().dayOfWeek
    var selected by rememberSaveable { mutableStateOf(if (today == DayOfWeek.SUNDAY) DayOfWeek.MONDAY else today) }
    val lessons = schedule.lessonsFor(selected)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Расписание", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.text)
        }
        item {
            Text(
                "Сейчас стоит тестовое расписание. Настоящее добавим в следующей версии.",
                fontSize = 14.sp,
                color = c.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
                    .padding(14.dp),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                schoolDays.forEach { day ->
                    val isSel = day == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) c.blue else c.card)
                            .selectable(selected = isSel, role = Role.Tab, onClick = { selected = day }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            day.shortRu(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSel) c.onAccent else if (day == today) c.blue else c.text,
                        )
                    }
                }
            }
        }
        if (lessons.isEmpty()) {
            item { Text("Уроков нет — выходной", color = c.muted, modifier = Modifier.padding(4.dp)) }
        }
        items(lessons, key = { it.number }) { lesson ->
            LessonRow(lesson, highlighted = false, past = false, accent = c.blue, tint = c.blueTint)
        }
    }
}
