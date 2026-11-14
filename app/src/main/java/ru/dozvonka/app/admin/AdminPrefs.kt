package ru.dozvonka.app.admin

import android.content.Context
import java.security.MessageDigest

/** PIN и ключ GitHub админа. Хранятся только на этом телефоне. */
object AdminPrefs {
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("admin", Context.MODE_PRIVATE)

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("dozvonka:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun hasPin(c: Context) = sp(c).contains("pin")
    fun setPin(c: Context, pin: String) = sp(c).edit().putString("pin", hash(pin)).apply()
    fun checkPin(c: Context, pin: String) = sp(c).getString("pin", null) == hash(pin)

    fun token(c: Context): String? = sp(c).getString("token", null)?.takeIf { it.isNotBlank() }
    fun setToken(c: Context, token: String?) = sp(c).edit().putString("token", token?.trim()).apply()
}
