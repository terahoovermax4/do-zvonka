package ru.dozvonka.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ru.dozvonka.app.BuildConfig
import ru.dozvonka.app.admin.AdminPrefs
import ru.dozvonka.app.notify.BellNotification
import ru.dozvonka.app.oem.OemHelper
import ru.dozvonka.app.prefs.Prefs
import ru.dozvonka.app.sched.BellScheduler
import ru.dozvonka.app.sched.BellUpdater
import ru.dozvonka.app.widget.SmallWidgetProvider
import ru.dozvonka.app.widget.WideWidgetProvider

@Composable
fun SettingsScreen(onOpenAdmin: () -> Unit) {
    val context = LocalContext.current
    val c = LocalAppColors.current
    val brand = remember { OemHelper.brand() }

    // Перепроверяем статусы каждый раз, когда возвращаемся из настроек телефона.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val notifOk = remember(resumes) { BellNotification.canPost(context) }
    val exactOk = remember(resumes) { BellScheduler.canExact(context) }
    val batteryOk = remember(resumes) { OemHelper.isIgnoringBatteryOptimizations(context) }

    var notifEnabled by remember { mutableStateOf(Prefs.notificationEnabled(context)) }
    var boost by remember { mutableStateOf(Prefs.boostMode(context)) }
    var simple by remember { mutableStateOf(Prefs.simpleNotification(context)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Настройки", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.text) }

        item { SectionTitle("Таймер") }
        item {
            SwitchRow("Таймер в уведомлении", "Виден на экране блокировки и в шторке", notifEnabled) {
                notifEnabled = it
                Prefs.setNotificationEnabled(context, it)
                BellUpdater.refresh(context)
            }
        }
        item {
            SwitchRow(
                "Усиленный режим",
                "Не даёт телефону закрыть таймер во время уроков. Нужен на Vivo, iQOO, Tecno, Infinix, Xiaomi. Батареи тратит чуть больше.",
                boost,
            ) {
                boost = it
                Prefs.setBoostMode(context, it)
                BellUpdater.refresh(context)
            }
        }
        if (Build.VERSION.SDK_INT < 36) {
            item {
                SwitchRow(
                    "Простое уведомление",
                    "Включите, если на вашем телефоне крупный таймер в уведомлении выглядит криво",
                    simple,
                ) {
                    simple = it
                    Prefs.setSimpleNotification(context, it)
                    BellUpdater.refresh(context)
                }
            }
        }

        item { SectionTitle("Чтобы таймер не пропадал · ${brand.title}") }
        item {
            SetupItem(
                title = "Уведомления",
                text = "Без них таймера на экране блокировки не будет.",
                ok = notifOk,
                button = "Открыть",
            ) { OemHelper.openNotificationSettings(context) }
        }
        item {
            SetupItem(
                title = "Точное время звонков",
                text = "Чтобы урок сменялся переменой секунда в секунду.",
                ok = exactOk,
                button = if (exactOk) null else "Разрешить",
            ) { OemHelper.openExactAlarmSettings(context) }
        }
        item {
            SetupItem(
                title = "Батарея без ограничений",
                text = "Система не будет усыплять приложение.",
                ok = batteryOk,
                button = if (batteryOk) null else "Разрешить",
            ) { OemHelper.openBatteryOptimization(context) }
        }
        item {
            SetupItem(
                title = "Автозапуск и работа в фоне",
                text = OemHelper.backgroundTip(),
                ok = null,
                button = "Открыть настройки",
            ) { OemHelper.openBackgroundSettings(context) }
        }
        item {
            SetupItem(
                title = "Экран блокировки",
                text = OemHelper.lockScreenTip(),
                ok = null,
                button = "Настройки уведомлений",
            ) { OemHelper.openNotificationSettings(context) }
        }
        if (OemHelper.needsRecentsLock()) {
            item { SetupItem("Закрепить в недавних", OemHelper.RECENTS_TIP, ok = null, button = null) {} }
        }
        if (Build.VERSION.SDK_INT >= 36) {
            item {
                SetupItem(
                    title = "Таймер в строке состояния",
                    text = "Android 16: в настройках уведомлений приложения включите «Показывать в реальном времени» (Live updates).",
                    ok = null,
                    button = "Открыть",
                ) { OemHelper.openNotificationSettings(context) }
            }
        }

        item { SectionTitle("Виджеты") }
        item {
            WidgetCard(context)
        }
        item { VersionLine(onOpenAdmin) }
    }
}

/** Строка с версией. 7 нажатий подряд — вход в админ-меню (с PIN-кодом). */
@Composable
private fun VersionLine(onOpenAdmin: () -> Unit) {
    val context = LocalContext.current
    val c = LocalAppColors.current
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }
    var askPin by remember { mutableStateOf(false) }

    Text(
        "До звонка · версия ${BuildConfig.VERSION_NAME}",
        fontSize = 13.sp,
        color = c.muted,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                val t = System.currentTimeMillis()
                taps = if (t - lastTap < 1500) taps + 1 else 1
                lastTap = t
                if (taps >= 7) {
                    taps = 0
                    askPin = true
                }
            }
            .padding(vertical = 16.dp),
    )

    if (askPin) {
        PinDialog(
            creating = !AdminPrefs.hasPin(context),
            onDismiss = { askPin = false },
            onSubmit = { pin ->
                if (!AdminPrefs.hasPin(context)) {
                    AdminPrefs.setPin(context, pin)
                    askPin = false
                    onOpenAdmin()
                    true
                } else if (AdminPrefs.checkPin(context, pin)) {
                    askPin = false
                    onOpenAdmin()
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun PinDialog(creating: Boolean, onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "Придумайте PIN админа" else "PIN админа") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creating) Text("Минимум 4 цифры. Он защищает меню на этом телефоне.", fontSize = 14.sp)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> pin = v.filter { it.isDigit() }.take(12); error = null },
                    singleLine = true,
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "Слишком короткий"
                    !onSubmit(pin) -> error = "Неверный PIN"
                }
            }) { Text("Войти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    val c = LocalAppColors.current
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = c.text,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text)
            Text(sub, fontSize = 13.sp, color = c.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = c.blue),
        )
    }
}

@Composable
private fun SetupItem(title: String, text: String, ok: Boolean?, button: String?, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val (statusText, statusColor) = when (ok) {
        true -> "Готово" to c.ok
        false -> "Нужно включить" to c.orange
        null -> "Проверьте вручную" to c.muted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f))
            Text(statusText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }
        Text(text, fontSize = 14.sp, color = c.muted)
        if (button != null) {
            if (ok == false) {
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = c.blue, contentColor = c.onAccent)) {
                    Text(button)
                }
            } else {
                OutlinedButton(onClick = onClick) { Text(button, color = c.blue) }
            }
        }
    }
}

@Composable
private fun WidgetCard(context: Context) {
    val c = LocalAppColors.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val canPin = remember { manager.isRequestPinAppWidgetSupported }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Зажмите пустое место на рабочем столе → Виджеты → «До звонка». " +
                "На Android 16 (Pixel) виджет можно поставить и на экран блокировки.",
            fontSize = 14.sp,
            color = c.muted,
        )
        if (canPin) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    manager.requestPinAppWidget(ComponentName(context, SmallWidgetProvider::class.java), null, null)
                }) { Text("Маленький", color = c.blue) }
                OutlinedButton(onClick = {
                    manager.requestPinAppWidget(ComponentName(context, WideWidgetProvider::class.java), null, null)
                }) { Text("Широкий", color = c.blue) }
            }
        }
    }
}
