#!/usr/bin/env bash
# Проверка на эмуляторе: ставим APK, открываем экраны, ищем падения, делаем скриншоты.
set -u
PKG=ru.dozvonka.app
APK=app/build/outputs/apk/release/app-release.apk
mkdir -p screens
exec > >(tee -a screens/smoke.log) 2>&1
fail=0

shot() { adb exec-out screencap -p > "screens/$1.png"; }

tap_text() { # нажать на элемент с текстом $1 (ищем через uiautomator)
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re,sys
t=sys.argv[1]; s=open('/tmp/ui.xml',encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', s):
    n=m.group(0)
    if f'text="{t}"' in n or f'content-desc="{t}"' in n:
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        x1,y1,x2,y2=map(int,b.groups()); print((x1+x2)//2,(y1+y2)//2); break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; else echo "Не нашёл «$1»"; fi
}

crashed() {
  adb logcat -d | grep -A30 -E "FATAL EXCEPTION|ANR in $PKG" > screens/crash.txt
  [ -s screens/crash.txt ]
}

adb install -r "$APK" || { echo "install failed"; exit 1; }
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb logcat -c

# 1. Как сейчас (по настоящим часам эмулятора)
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 6; shot 1-now

# 2. Вторник 10:05 — большая перемена; Вт 08:47 — идёт урок
adb root >/dev/null 2>&1; sleep 2
adb shell settings put global auto_time 0
adb shell am force-stop $PKG
adb shell date 111708472026.00 >/dev/null 2>&1 || adb shell su 0 date 111708472026.00
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 6; shot 2-lesson
adb shell cmd statusbar expand-notifications; sleep 3; shot 3-notification
adb shell cmd statusbar collapse; sleep 1

adb shell date 111710052026.00 >/dev/null 2>&1 || adb shell su 0 date 111710052026.00
sleep 2
adb shell am start -W -n $PKG/.ui.MainActivity
sleep 5; shot 4-break

# 3. Вкладки
tap_text "Расписание"; sleep 3; shot 5-schedule
tap_text "Настройки"; sleep 3; shot 6-settings

# 4. Экран блокировки (ставим PIN, иначе блокировки нет) — виден ли таймер
adb shell am start -W -n $PKG/.ui.MainActivity >/dev/null; sleep 1
tap_text "Сегодня"; sleep 1
adb shell locksettings set-pin 1111 >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_SLEEP; sleep 3
adb shell input keyevent KEYCODE_WAKEUP; sleep 3; shot 7-lockscreen
adb shell locksettings clear --old 1111 >/dev/null 2>&1 || true

if crashed; then echo "ПАДЕНИЕ:"; cat screens/crash.txt; fail=1; fi
adb shell dumpsys meminfo $PKG | grep -E "TOTAL PSS|TOTAL:" > screens/memory.txt || true
ls -la screens
exit $fail
