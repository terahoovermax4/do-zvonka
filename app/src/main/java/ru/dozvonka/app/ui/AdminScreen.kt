package ru.dozvonka.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dozvonka.app.admin.AdminPrefs
import ru.dozvonka.app.admin.GitHubApi
import ru.dozvonka.app.core.hm
import ru.dozvonka.app.core.parseDateRu
import ru.dozvonka.app.core.parseDateTimeRu
import ru.dozvonka.app.core.parseTimeRu
import ru.dozvonka.app.core.ru
import ru.dozvonka.app.core.shortRu
import ru.dozvonka.app.core.shortenedDay
import ru.dozvonka.app.data.Announcement
import ru.dozvonka.app.data.DayOverride
import ru.dozvonka.app.data.Lesson
import ru.dozvonka.app.data.RemoteConfig
import ru.dozvonka.app.data.ScheduleRepository
import ru.dozvonka.app.prefs.AppClock
import ru.dozvonka.app.sched.BellUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val DT_SHOW = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm")

/** Строка урока в редакторе — всё текстом, чтобы можно было спокойно печатать. */
private data class LessonDraft(val n: String, val subject: String, val start: String, val end: String, val room: String) {
    fun toLesson(): Lesson? {
        val s = parseTimeRu(start) ?: return null
        val e = parseTimeRu(end) ?: return null
        if (!e.isAfter(s) || subject.isBlank()) return null
        return Lesson(n.trim().toIntOrNull() ?: return null, subject.trim(), room.trim(), s, e)
    }

    companion object {
        fun of(l: Lesson) = LessonDraft(l.number.toString(), l.subject, l.start.hm(), l.end.hm(), l.room)
    }
}

private val weekDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
fun AdminScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val config by remember { ScheduleRepository.flow(context) }.collectAsState()

    var token by remember { mutableStateOf(AdminPrefs.token(context)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    /** Отправить новую версию расписания всем. Старые объявления (>7 дней) заодно чистим. */
    fun publish(newConfig: RemoteConfig, commitMessage: String, okText: String, onOk: () -> Unit = {}) {
        val t = token
        if (t == null) {
            status = "Сначала добавьте ключ GitHub (первый раздел)."
            return
        }
        val weekAgo = LocalDate.now().minusDays(7)
        val cleaned = newConfig.copy(
            announcements = newConfig.announcements.filter { !it.until.isBefore(weekAgo) },
            schedule = newConfig.schedule.copy(
                overrides = newConfig.schedule.overrides.filterKeys { !it.isBefore(weekAgo) },
            ),
        )
        busy = true
        status = "Публикую…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                GitHubApi.publish(context.applicationContext, t, cleaned, commitMessage)
            }
            busy = false
            r.onSuccess { status = okText; onOk() }
                .onFailure { status = it.message ?: "Не получилось опубликовать." }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("← Назад", color = c.blue) }
            Text("Админ-меню", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.text)
        }
        status?.let {
            Text(
                it,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.text,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.blueTint)
                    .padding(12.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TokenSection(
                    token = token,
                    busy = busy,
                    onSave = { newToken ->
                        busy = true
                        status = "Проверяю ключ…"
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { GitHubApi.checkToken(newToken) }
                            busy = false
                            r.onSuccess {
                                AdminPrefs.setToken(context, newToken)
                                token = newToken
                                status = it
                            }.onFailure { status = it.message }
                        }
                    },
                    onDelete = {
                        AdminPrefs.setToken(context, null)
                        token = null
                        status = "Ключ удалён с этого телефона."
                    },
                )
            }
            item { TestClockSection(onChanged = { status = it }) }
            item {
                AnnouncementSection(config, busy) { cfg, msg, ok, done -> publish(cfg, msg, ok, done) }
            }
            item {
                SpecialDaySection(config, busy) { cfg, msg, ok, done -> publish(cfg, msg, ok, done) }
            }
            item {
                WeekEditorSection(config, busy) { cfg, msg, ok, done -> publish(cfg, msg, ok, done) }
            }
            item { StatsSection() }
        }
    }
}

private typealias Publish = (RemoteConfig, String, String, () -> Unit) -> Unit

@Composable
private fun AdminCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.text)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, fontSize = 13.sp, color = LocalAppColors.current.muted)
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = c.blue, contentColor = c.onAccent),
    ) { Text(text) }
}

// ---------------- Ключ GitHub ----------------

@Composable
private fun TokenSection(token: String?, busy: Boolean, onSave: (String) -> Unit, onDelete: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AdminCard("Ключ GitHub") {
        if (token == null) {
            Hint(
                "Нужен, чтобы публиковать расписание и объявления для всех. Хранится только на этом телефоне.\n" +
                    "1. github.com → Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token.\n" +
                    "2. Repository access: Only select repositories → do-zvonka.\n" +
                    "3. Permissions → Contents: Read and write.\n" +
                    "4. Скопируйте ключ (github_pat_…) и вставьте сюда.",
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim() },
                singleLine = true,
                label = { Text("github_pat_…") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Сохранить и проверить", enabled = !busy && input.length > 20) { onSave(input) }
        } else {
            Hint("Ключ сохранён на этом телефоне: …${token.takeLast(4)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSave(token) }, enabled = !busy) { Text("Проверить") }
                OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Удалить ключ") }
            }
        }
    }
}

// ---------------- Тестовое время ----------------

@Composable
private fun TestClockSection(onChanged: (String) -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun set(t: LocalDateTime?) {
        AppClock.setFakeNow(context, t)
        BellUpdater.refresh(context)
        tick++
        onChanged(if (t == null) "Тестовое время выключено." else "Тестовое время: ${t.format(DT_SHOW)}. Смотрите уведомление и виджеты.")
    }

    val realToday = LocalDate.now()
    fun next(day: DayOfWeek, time: String): LocalDateTime =
        realToday.with(TemporalAdjusters.nextOrSame(day)).atTime(LocalTime.parse(time))

    AdminCard("Тестовое время (только этот телефон)") {
        val clockText = remember(tick) {
            if (AppClock.isFake) "Включено. Для приложения сейчас: ${AppClock.now().format(DT_SHOW)}"
            else "Сейчас настоящее время. Можно «перенестись» в любой момент, чтобы проверить таймер."
        }
        Hint(clockText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { set(next(DayOfWeek.MONDAY, "08:20")) }) { Text("Пн 08:20") }
            OutlinedButton(onClick = { set(next(DayOfWeek.TUESDAY, "10:05")) }) { Text("Вт 10:05") }
            OutlinedButton(onClick = { set(next(DayOfWeek.THURSDAY, "14:35")) }) { Text("Чт 14:35") }
        }
        Hint("Пн 08:20 — нулевой урок, Вт 10:05 — большая перемена, Чт 14:35 — конец последнего урока.")
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = null },
            singleLine = true,
            label = { Text("ДД.ММ.ГГГГ ЧЧ:ММ") },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Включить") {
                val t = parseDateTimeRu(input)
                if (t == null) error = "Например: 17.11.2026 10:05" else set(t)
            }
            OutlinedButton(onClick = { set(null) }, enabled = AppClock.isFake) { Text("Выключить") }
        }
    }
}

// ---------------- Объявления ----------------

@Composable
private fun AnnouncementSection(config: RemoteConfig, busy: Boolean, publish: Publish) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("3") }
    val active = config.announcements
        .filter { !it.until.isBefore(LocalDate.now()) }
        .sortedByDescending { it.created }

    AdminCard("Объявление всем") {
        OutlinedTextField(
            value = title, onValueChange = { title = it }, singleLine = true,
            label = { Text("Заголовок, например «Физики не будет»") }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = text, onValueChange = { text = it }, minLines = 2,
            label = { Text("Текст (можно пустой)") }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = days, onValueChange = { v -> days = v.filter { it.isDigit() }.take(2) }, singleLine = true,
            label = { Text("Сколько дней показывать") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(220.dp),
        )
        PrimaryButton("Отправить всем", enabled = !busy && title.isNotBlank()) {
            val now = LocalDateTime.now().withNano(0)
            val a = Announcement(
                id = "a" + System.currentTimeMillis(),
                title = title.trim(),
                text = text.trim(),
                created = now,
                until = now.toLocalDate().plusDays(((days.toIntOrNull() ?: 3).coerceIn(1, 60) - 1).toLong()),
            )
            publish(
                config.copy(announcements = config.announcements + a),
                "Объявление: ${a.title}",
                "Отправлено. У всех появится в течение ~15 минут (или сразу при открытии приложения).",
            ) { title = ""; text = "" }
        }
        if (active.isNotEmpty()) {
            Hint("Сейчас показываются:")
            active.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${a.title} (до ${a.until.ru()})",
                        fontSize = 14.sp,
                        color = LocalAppColors.current.text,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(enabled = !busy, onClick = {
                        publish(
                            config.copy(announcements = config.announcements.filter { it.id != a.id }),
                            "Убрано объявление: ${a.title}",
                            "Объявление убрано.",
                        ) {}
                    }) { Text("Убрать") }
                }
            }
        }
    }
}

// ---------------- Особый день ----------------

private enum class DayMode(val title: String, val defaultNote: String) {
    SHORT("Сокращённый", "Сокращённый день"),
    OFF("Уроков нет", "Уроков нет"),
    NOTE("Только заметка", ""),
}

@Composable
private fun SpecialDaySection(config: RemoteConfig, busy: Boolean, publish: Publish) {
    val c = LocalAppColors.current
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(1).ru()) }
    var mode by remember { mutableStateOf(DayMode.SHORT) }
    var startText by remember { mutableStateOf("") }
    var lessonMin by remember { mutableStateOf("30") }
    var breakMin by remember { mutableStateOf("10") }
    var note by remember { mutableStateOf(DayMode.SHORT.defaultNote) }
    var tellAll by remember { mutableStateOf(true) }

    val date = parseDateRu(dateText)
    val base = date?.let { config.schedule.lessonsFor(it.dayOfWeek) }.orEmpty()
    val start = parseTimeRu(startText) ?: base.firstOrNull()?.start
    val lm = lessonMin.toIntOrNull()?.takeIf { it in 5..90 }
    val bm = breakMin.toIntOrNull()?.takeIf { it in 0..60 }
    val preview: List<Lesson>? =
        if (mode == DayMode.SHORT && start != null && lm != null && bm != null) shortenedDay(base, start, lm, bm) else null

    AdminCard("Особый день") {
        OutlinedTextField(
            value = dateText, onValueChange = { dateText = it }, singleLine = true,
            label = { Text("Дата ДД.ММ.ГГГГ") }, isError = date == null,
            supportingText = { if (date != null) Text(date.dayOfWeek.shortRu() + ", обычно уроков: ${base.size}") },
            modifier = Modifier.width(240.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayMode.values().forEach { m ->
                val sel = m == mode
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) c.blue else c.bg)
                        .selectable(selected = sel, role = Role.RadioButton, onClick = { mode = m; note = m.defaultNote })
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) { Text(m.title, fontSize = 13.sp, color = if (sel) c.onAccent else c.text) }
            }
        }
        if (mode == DayMode.SHORT) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText, onValueChange = { startText = it }, singleLine = true,
                    label = { Text("Начало") }, placeholder = { Text(base.firstOrNull()?.start?.hm() ?: "08:30") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = lessonMin, onValueChange = { v -> lessonMin = v.filter { it.isDigit() }.take(2) }, singleLine = true,
                    label = { Text("Урок, мин") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = breakMin, onValueChange = { v -> breakMin = v.filter { it.isDigit() }.take(2) }, singleLine = true,
                    label = { Text("Перем., мин") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            if (preview != null && preview.isNotEmpty()) {
                Hint(preview.joinToString("\n") { "${it.number}. ${it.start.hm()}–${it.end.hm()}  ${it.subject}" })
            }
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it }, singleLine = true,
            label = { Text("Подпись на главном экране") }, modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = tellAll, onCheckedChange = { tellAll = it })
            Text("Сообщить всем уведомлением", fontSize = 14.sp, color = c.text)
        }
        val ready = date != null && (mode != DayMode.SHORT || (preview != null && preview.isNotEmpty())) &&
            (mode != DayMode.NOTE || note.isNotBlank())
        PrimaryButton("Опубликовать", enabled = !busy && ready) {
            val d = date ?: return@PrimaryButton
            val lessons = when (mode) {
                DayMode.SHORT -> preview
                DayMode.OFF -> emptyList()
                DayMode.NOTE -> null
            }
            val override = DayOverride(note.trim(), lessons)
            var newConfig = config.copy(
                schedule = config.schedule.copy(overrides = config.schedule.overrides + (d to override)),
            )
            if (tellAll) {
                val summary = when (mode) {
                    DayMode.SHORT -> "Уроки по $lm мин, перемены по $bm мин: ${preview?.firstOrNull()?.start?.hm()}–${preview?.lastOrNull()?.end?.hm()}"
                    DayMode.OFF -> "Уроков не будет."
                    DayMode.NOTE -> ""
                }
                val now = LocalDateTime.now().withNano(0)
                newConfig = newConfig.copy(
                    announcements = newConfig.announcements + Announcement(
                        id = "d" + System.currentTimeMillis(),
                        title = "${d.ru()} (${d.dayOfWeek.shortRu()}): ${note.trim().ifBlank { mode.defaultNote }}",
                        text = summary,
                        created = now,
                        until = d,
                    ),
                )
            }
            publish(newConfig, "Особый день $d: ${mode.title}", "Опубликовано. Расписание на ${d.ru()} обновится у всех.") {}
        }

        val today = LocalDate.now()
        val upcoming = config.schedule.overrides.entries
            .filter { !it.key.isBefore(today) }
            .sortedBy { it.key.toEpochDay() }
        if (upcoming.isNotEmpty()) {
            Hint("Уже назначено:")
            upcoming.forEach { entry ->
                val d: LocalDate = entry.key
                val o: DayOverride = entry.value
                val ls = o.lessons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val kind = when {
                        ls == null -> "заметка"
                        ls.isEmpty() -> "уроков нет"
                        else -> "${ls.first().start.hm()}–${ls.last().end.hm()}"
                    }
                    Text(
                        "${d.ru()} ${d.dayOfWeek.shortRu()} — ${o.note.ifBlank { kind }} ($kind)",
                        fontSize = 14.sp, color = c.text, modifier = Modifier.weight(1f),
                    )
                    TextButton(enabled = !busy, onClick = {
                        publish(
                            config.copy(schedule = config.schedule.copy(overrides = config.schedule.overrides - d)),
                            "Убран особый день $d",
                            "${d.ru()} снова обычный день.",
                        ) {}
                    }) { Text("Убрать") }
                }
            }
        }
    }
}

// ---------------- Редактор недели ----------------

@Composable
private fun WeekEditorSection(config: RemoteConfig, busy: Boolean, publish: Publish) {
    val c = LocalAppColors.current
    var day by remember { mutableStateOf(DayOfWeek.MONDAY) }
    // Черновик для выбранного дня; сбрасывается при смене дня или нового расписания.
    val draft = remember(day, config.updatedAt) {
        mutableStateListOf<LessonDraft>().apply { addAll(config.schedule.lessonsFor(day).map(LessonDraft::of)) }
    }
    var error by remember(day) { mutableStateOf<String?>(null) }

    AdminCard("Расписание недели (для всех)") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            weekDays.forEach { d ->
                val sel = d == day
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) c.blue else c.bg)
                        .selectable(selected = sel, role = Role.Tab, onClick = { day = d }),
                    contentAlignment = Alignment.Center,
                ) { Text(d.shortRu(), fontSize = 14.sp, color = if (sel) c.onAccent else c.text) }
            }
        }
        if (draft.isEmpty()) Hint("В этот день уроков нет. Можно добавить.")

        draft.forEachIndexed { i, l ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.bg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = l.n, onValueChange = { v -> draft[i] = l.copy(n = v.filter { it.isDigit() }.take(2)) },
                        singleLine = true, label = { Text("№") }, modifier = Modifier.width(64.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = l.subject, onValueChange = { draft[i] = l.copy(subject = it) },
                        singleLine = true, label = { Text("Предмет") }, modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = l.start, onValueChange = { draft[i] = l.copy(start = it) },
                        singleLine = true, label = { Text("Начало") }, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = l.end, onValueChange = { draft[i] = l.copy(end = it) },
                        singleLine = true, label = { Text("Конец") }, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = l.room, onValueChange = { draft[i] = l.copy(room = it) },
                        singleLine = true, label = { Text("Каб.") }, modifier = Modifier.weight(0.8f),
                    )
                }
                TextButton(onClick = { draft.removeAt(i) }) { Text("Удалить урок", color = c.orange) }
            }
        }

        OutlinedButton(onClick = {
            val last = draft.lastOrNull()?.toLesson()
            val start = last?.end?.plusMinutes(10) ?: LocalTime.of(8, 30)
            draft.add(
                LessonDraft(
                    n = ((last?.number ?: 0) + 1).toString(),
                    subject = "",
                    start = start.hm(),
                    end = start.plusMinutes(40).hm(),
                    room = "",
                ),
            )
        }) { Text("+ Добавить урок") }

        error?.let { Text(it, color = c.orange, fontSize = 14.sp) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Опубликовать ${day.shortRu()}", enabled = !busy) {
                val lessons = draft.map { it.toLesson() }
                val bad = lessons.indexOfFirst { it == null }
                if (bad >= 0) {
                    error = "Урок ${bad + 1}: проверьте номер, предмет и время (ЧЧ:ММ, конец позже начала)."
                    return@PrimaryButton
                }
                val sorted = lessons.filterNotNull().sortedBy { it.start }
                val overlap = sorted.zipWithNext().firstOrNull { (a, b) -> b.start.isBefore(a.end) }
                if (overlap != null) {
                    error = "Уроки ${overlap.first.number} и ${overlap.second.number} пересекаются по времени."
                    return@PrimaryButton
                }
                error = null
                val days = config.schedule.days.toMutableMap()
                if (sorted.isEmpty()) days.remove(day) else days[day] = sorted
                publish(
                    config.copy(schedule = config.schedule.copy(days = days)),
                    "Расписание: ${day.shortRu()}",
                    "Расписание на ${day.shortRu()} обновлено у всех.",
                ) {}
            }
            OutlinedButton(onClick = {
                draft.clear()
                draft.addAll(config.schedule.lessonsFor(day).map(LessonDraft::of))
                error = null
            }) { Text("Отменить") }
        }
    }
}

// ---------------- Статистика ----------------

@Composable
private fun StatsSection() {
    var downloads by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        downloads = "загружаю…"
        downloads = withContext(Dispatchers.IO) { GitHubApi.downloads() }
            .fold({ "$it" }, { "не удалось узнать (${it.message})" })
    }
    AdminCard("Статистика") {
        Text("APK скачали: ${downloads ?: "…"}", fontSize = 16.sp, color = LocalAppColors.current.text)
        Hint("Считаются скачивания по ссылке с GitHub. Если файл пересылали в Telegram, это сюда не попадёт.")
        Hint("Список устройств (кто установил, модель телефона, версия) появится после подключения сервера — это этап 2.")
        OutlinedButton(onClick = { reload++ }) { Text("Обновить") }
    }
}
