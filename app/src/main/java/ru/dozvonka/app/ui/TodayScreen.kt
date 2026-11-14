package ru.dozvonka.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.dozvonka.app.core.BellContent
import ru.dozvonka.app.core.BellState
import ru.dozvonka.app.core.ScheduleEngine
import ru.dozvonka.app.core.dayTitle
import ru.dozvonka.app.core.formatCountdown
import ru.dozvonka.app.core.hm
import ru.dozvonka.app.data.Lesson
import ru.dozvonka.app.data.Announcement
import ru.dozvonka.app.data.RemoteConfig
import ru.dozvonka.app.notify.Announcements
import ru.dozvonka.app.prefs.AppClock
import java.time.LocalDateTime

/** Текущее время, обновляется ровно на каждой секунде. */
@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(AppClock.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val n = AppClock.now()
            now = n
            delay(1000L - n.nano / 1_000_000)
        }
    }
    return now
}

@Composable
fun TodayScreen(config: RemoteConfig) {
    val c = LocalAppColors.current
    val now = rememberNow()
    val schedule = config.schedule
    val note = schedule.noteOn(now.toLocalDate())
    val announcements = Announcements.active(config.announcements)
    val state = ScheduleEngine.stateAt(schedule, now)
    val content = BellContent.from(state)
    val isBreak = content?.isBreak == true
    val accent = if (isBreak) c.orange else c.blue
    val tint = if (isBreak) c.orangeTint else c.blueTint
    val lessons = schedule.lessonsOn(now.toLocalDate())
    val highlighted: Int? = when (state) {
        is BellState.InLesson -> state.lesson.number
        is BellState.OnBreak -> state.next.number
        is BellState.BeforeSchool -> state.first.number
        else -> null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (AppClock.isFake) {
            item { Banner("Тестовое время — только на этом телефоне. Выключить: Настройки → админ-меню.", c.orangeTint, c.orange) }
        }
        item { Header(now, content?.chip, accent, tint) }
        if (note != null) {
            item { Banner(note, c.orangeTint, c.orange) }
        }
        items(announcements, key = { "ann-" + it.id }) { a -> AnnouncementCard(a) }

        if (content != null) {
            item {
                CountdownRing(
                    progress = content.progress(now),
                    countdown = formatCountdown(content.remaining(now)),
                    label = content.countdownLabel,
                    accent = accent,
                )
            }
            item { NowNextCard(content, accent) }
        } else {
            item {
                val (title, sub) = BellContent.idleText(state, now)
                IdleCard(title, sub)
            }
        }

        item {
            Text(
                "Расписание на день",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = c.text,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        if (lessons.isEmpty()) {
            item { Text("Сегодня уроков нет", color = c.muted, modifier = Modifier.padding(start = 4.dp)) }
        }
        items(lessons, key = { it.number }) { lesson ->
            val hot = lesson.number == highlighted
            val past = !hot && !lesson.end.isAfter(now.toLocalTime())
            LessonRow(lesson, highlighted = hot, past = past, accent = accent, tint = tint)
        }
    }
}

@Composable
private fun Banner(text: String, bg: Color, fg: Color) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun AnnouncementCard(a: Announcement) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(c.orange))
        Column(Modifier.weight(1f)) {
            Text(a.title.ifBlank { "Объявление" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.text)
            if (a.text.isNotBlank()) Text(a.text, fontSize = 14.sp, color = c.muted)
        }
    }
}

@Composable
private fun Header(now: LocalDateTime, chip: String?, accent: Color, tint: Color) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(now.toLocalDate().dayTitle(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.muted)
            Text("Сегодня", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.text)
        }
        if (chip != null) {
            Text(
                chip,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun CountdownRing(progress: Float, countdown: String, label: String, accent: Color) {
    val c = LocalAppColors.current
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(c.track, 0f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(stroke))
                drawArc(
                    accent, -90f, 360f * progress, false,
                    topLeft = topLeft, size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    countdown,
                    color = c.text,
                    style = TextStyle(
                        fontSize = if (countdown.length > 5) 44.sp else 60.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.muted)
            }
        }
    }
}

@Composable
private fun NowNextCard(content: BellContent, accent: Color) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NowNextRow("Сейчас", content.title, content.details, accent, bold = true)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        NowNextRow("Дальше", content.nextTitle, content.nextSub, c.track, bold = false)
    }
}

@Composable
private fun NowNextRow(caption: String, title: String, sub: String, bar: Color, bold: Boolean) {
    val c = LocalAppColors.current
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(6.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(bar))
        Column {
            Text(caption.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.muted, letterSpacing = 0.6.sp)
            Text(title, fontSize = if (bold) 17.sp else 16.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = c.text)
            Text(sub, fontSize = 14.sp, color = c.muted)
        }
    }
}

@Composable
private fun IdleCard(title: String, sub: String) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.text)
        Text(sub, fontSize = 15.sp, color = c.muted)
    }
}

@Composable
fun LessonRow(lesson: Lesson, highlighted: Boolean, past: Boolean, accent: Color, tint: Color) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) tint else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        highlighted -> accent
                        past -> c.track
                        else -> c.card
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${lesson.number}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    highlighted -> c.onAccent
                    past -> c.muted
                    else -> c.text
                },
            )
        }
        Column(Modifier.weight(1f)) {
            Text(lesson.subject, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (past) c.muted else c.text)
            if (lesson.roomLabel.isNotBlank()) Text(lesson.roomLabel, fontSize = 12.sp, color = c.muted)
        }
        Text(
            "${lesson.start.hm()}–${lesson.end.hm()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) accent else c.muted,
        )
    }
}
