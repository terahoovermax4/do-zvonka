package ru.dozvonka.app.prefs

import android.content.Context
import ru.dozvonka.app.oem.OemHelper

object Prefs {
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Показывать уведомление с таймером. */
    fun notificationEnabled(c: Context) = sp(c).getBoolean("notif", true)
    fun setNotificationEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("notif", v).apply()

    /** Обычное системное уведомление без своей вёрстки (если на каком-то телефоне криво). */
    fun simpleNotification(c: Context) = sp(c).getBoolean("simple", false)
    fun setSimpleNotification(c: Context, v: Boolean) = sp(c).edit().putBoolean("simple", v).apply()

    /**
     * Усиленный режим: во время уроков работает «службой переднего плана»,
     * которую Vivo/Tecno/Xiaomi и др. почти не убивают. На таких телефонах включён сразу.
     */
    fun boostMode(c: Context) = sp(c).getBoolean("boost", OemHelper.boostByDefault())
    fun setBoostMode(c: Context, v: Boolean) = sp(c).edit().putBoolean("boost", v).apply()
}
