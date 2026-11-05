package ru.dozvonka.app.oem

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Разные производители по-разному «убивают» приложения в фоне.
 * Здесь: определяем марку телефона, даём понятную инструкцию
 * и пробуем открыть нужный экран настроек.
 */
object OemHelper {

    enum class Brand(val title: String) {
        SAMSUNG("Samsung"),
        VIVO("Vivo / iQOO"),
        TRANSSION("Tecno / Infinix / itel"),
        XIAOMI("Xiaomi / Redmi / POCO"),
        OPPO("OPPO / realme / OnePlus"),
        HUAWEI("Huawei / Honor"),
        OTHER("Android"),
    }

    fun brand(): Brand {
        val m = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return when {
            "samsung" in m -> Brand.SAMSUNG
            "vivo" in m || "iqoo" in m -> Brand.VIVO
            "tecno" in m || "infinix" in m || "itel" in m || "transsion" in m -> Brand.TRANSSION
            "xiaomi" in m || "redmi" in m || "poco" in m -> Brand.XIAOMI
            "oppo" in m || "realme" in m || "oneplus" in m -> Brand.OPPO
            "huawei" in m || "honor" in m -> Brand.HUAWEI
            else -> Brand.OTHER
        }
    }

    /** На «агрессивных» прошивках усиленный режим включаем сразу. */
    fun boostByDefault(): Boolean = brand() in setOf(
        Brand.VIVO, Brand.TRANSSION, Brand.XIAOMI, Brand.OPPO, Brand.HUAWEI,
    )

    /** Нужно ли закреплять приложение в списке недавних. */
    fun needsRecentsLock(): Boolean = brand() in setOf(
        Brand.VIVO, Brand.TRANSSION, Brand.XIAOMI, Brand.OPPO, Brand.HUAWEI,
    )

    // ---------- Тексты ----------

    fun backgroundTip(): String = when (brand()) {
        Brand.SAMSUNG ->
            "Настройки → Батарея → Ограничения фоновой работы → «Приложения, которые никогда не будут спать» → добавьте «До звонка». " +
                "Проверьте, что его нет в списках «Спящие» и «Глубокий сон»."
        Brand.VIVO ->
            "Разрешите автозапуск: i Manager → Управление приложениями → Автозапуск → включите «До звонка». " +
                "Затем Настройки → Батарея → Потребление энергии в фоне → «До звонка» → разрешить работу в фоне."
        Brand.TRANSSION ->
            "Phone Master → Управление приложениями → Автозапуск → включите «До звонка». " +
                "Затем Настройки → Батарея → Управление энергопотреблением приложений → «До звонка» → «Без ограничений»."
        Brand.XIAOMI ->
            "Настройки → Приложения → «До звонка»: включите «Автозапуск», а в разделе «Контроль активности» выберите «Нет ограничений»."
        Brand.OPPO ->
            "Настройки → Приложения → «До звонка» → Использование батареи: разрешите работу в фоне и автозапуск."
        Brand.HUAWEI ->
            "Настройки → Батарея → Запуск приложений → «До звонка»: выключите автоуправление и включите все три переключателя."
        Brand.OTHER ->
            "Настройки → Приложения → «До звонка» → Батарея → «Без ограничений»."
    }

    fun lockScreenTip(): String = when (brand()) {
        Brand.SAMSUNG ->
            "Настройки → Экран блокировки → Уведомления: включите показ и выберите «Показывать содержимое»."
        Brand.VIVO ->
            "Настройки → Уведомления и строка состояния → Уведомления на экране блокировки → показывать полностью."
        Brand.TRANSSION ->
            "Настройки → Уведомления → Экран блокировки → показывать всё содержимое."
        else ->
            "Настройки → Уведомления → Уведомления на экране блокировки → показывать всё, включая «беззвучные»."
    }

    const val RECENTS_TIP =
        "Откройте список недавних приложений и закрепите «До звонка» (потяните карточку вниз или нажмите на замок). " +
            "Тогда кнопка «Очистить всё» его не закроет."

    // ---------- Проверки ----------

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    // ---------- Переходы в настройки ----------

    private fun cmp(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

    private fun backgroundIntents(): List<Intent> = when (brand()) {
        Brand.SAMSUNG -> listOf(
            cmp("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            cmp("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            cmp("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        )
        Brand.VIVO -> listOf(
            cmp("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            cmp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            cmp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            cmp("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"),
        )
        Brand.TRANSSION -> listOf(
            cmp("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
        )
        Brand.XIAOMI -> listOf(
            cmp("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        Brand.OPPO -> listOf(
            cmp("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            cmp("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            cmp("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        )
        Brand.HUAWEI -> listOf(
            cmp("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            cmp("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        )
        Brand.OTHER -> emptyList()
    }

    fun openBackgroundSettings(context: Context) = open(context, backgroundIntents())

    @SuppressLint("BatteryLife")
    fun openBatteryOptimization(context: Context) = open(
        context,
        listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        ),
    )

    fun openNotificationSettings(context: Context) = open(
        context,
        listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        ),
    )

    fun openExactAlarmSettings(context: Context) = open(
        context,
        if (Build.VERSION.SDK_INT >= 31) {
            listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        } else {
            emptyList()
        },
    )

    /** Пробуем экраны по очереди; если ни один не открылся — страница приложения в настройках. */
    fun open(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: Exception) {
                // Нет такого экрана на этой прошивке — пробуем следующий.
            }
        }
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }
}
