package ru.dozvonka.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import ru.dozvonka.app.R
import ru.dozvonka.app.data.ScheduleRepository
import ru.dozvonka.app.sched.BellUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DoZvonkaTheme { AppRoot() } }
    }

    override fun onResume() {
        super.onResume()
        // Открыли приложение — заодно всё пересчитали и перезавели будильники.
        BellUpdater.refresh(this)
    }
}

private data class Tab(val title: String, val icon: Int)

private val tabs = listOf(
    Tab("Сегодня", R.drawable.ic_tab_today),
    Tab("Расписание", R.drawable.ic_tab_schedule),
    Tab("Настройки", R.drawable.ic_tab_settings),
)

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val c = LocalAppColors.current
    val schedule = remember { ScheduleRepository.get(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        BellUpdater.refresh(context)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            NavigationBar(containerColor = c.card) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = c.blue,
                            selectedTextColor = c.blue,
                            indicatorColor = c.blueTint,
                            unselectedIconColor = c.muted,
                            unselectedTextColor = c.muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> TodayScreen(schedule)
                1 -> ScheduleScreen(schedule)
                else -> SettingsScreen()
            }
        }
    }
}
