# Nova TV (launcher) - setup on Windows

## 0. Identify your box first (nothing here changes the box)
Settings > About (or System > About): note Android version, model, build number.
With ADB (below), from a Windows terminal:
  adb shell getprop ro.build.version.release   (Android version)
  adb shell getprop ro.product.model
  adb shell getprop ro.board.platform          (chip family, e.g. rk322x / gxl)
  adb shell getprop ro.hardware
  adb shell getprop ro.build.display.id        (firmware build)
  adb shell "head -3 /proc/meminfo"            (RAM)
  adb shell df /data                           (storage)
  adb shell "grep -m1 Hardware /proc/cpuinfo"
Spec labels on these boxes are often inflated, so trust these outputs, not the box.

## 1. Get the tools (all free)
- Android Studio: https://developer.android.com/studio (includes JDK + SDK; accept SDK 34 when asked)
- ADB comes with it (platform-tools), or install "Android SDK Platform-Tools".

## 2. Build the APK
1. Android Studio > Open > select the NovaTV folder. Wait for Gradle sync.
   (If it offers an Android Gradle Plugin upgrade, accepting is fine.)
2. Build > Build Bundle(s)/APK(s) > Build APK(s).
3. Result: app/build/outputs/apk/debug/app-debug.apk

## 3. Install on the box
1. On the box: Settings > About > tap "Build number" 7 times; then Developer options > enable USB debugging.
2. Same Wi-Fi/LAN as the PC. Find the box IP in Settings > Network.
3. Terminal: adb connect BOX_IP:5555    (accept the prompt on the TV)
4. adb install -r app-debug.apk
   (or copy the APK to a USB stick and open it with the box's file manager)

## 4. Use it
- Open "Nova TV" from the app list. Arrows move, OK opens, Back goes up one level.
- Hold OK on an app to pin/unpin it as a favourite.
- To make it your home screen: Nova TV > Settings > Choose home app, or press Home and pick
  Nova TV > "Always". To undo: Android settings > Apps > Nova TV > Clear defaults.
  This is an ordinary app and a normal Android setting; it does not modify system files.

## Troubleshooting
- No apps listed: the box may block app queries; reinstall and make sure you installed the APK built from this project.
- "adb: no devices": re-check USB debugging, IP address, and run `adb kill-server`.
- Home key still opens the old launcher: clear the old launcher's defaults in Android settings > Apps.
- Edges cut off: adjust the 5% overscan margin in render() (px/py).
- Sluggish: Settings > Wallpaper is already a plain gradient; also close other apps and check free RAM.
