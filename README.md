# Aurora iOS Launcher (Android)

Aurora is a lightweight Android home-screen launcher starter project with an iOS-inspired design language, editable wallpaper, a lock-screen-style studio, and a frosted-glass Control Center.

## Included

- Home app that can be selected as Android's default Home app.
- Reads launchable app names/icons from Android and opens the selected installed app.
- Wallpaper presets and gallery photo import. The Android build attempts to apply a chosen image as the system and lock wallpaper; manufacturer restrictions may limit lock-wallpaper changes.
- Lock Screen Studio: drag the clock anywhere on the preview; adjust clock font, size, horizontal stretch, color and position; change the displayed city, temperature, and conditions; toggle preview notification cards and the date.
- Glass Control Center with Wi-Fi/Bluetooth settings shortcuts, flashlight control where available, and brightness/media-volume sliders.
- Browser preview in `app/src/main/assets/index.html`.
- No ads, analytics SDKs, trackers, continuously running service, or weather polling.

## Honest Android limitations

This is a launcher, not an operating-system replacement. Standard third-party launchers cannot restyle or replace the secure system lock-screen UI and Android notification shade universally. The lock screen inside Aurora is an editor/preview. A wallpaper can often be applied to the actual lock screen, but clock fonts, clock placement, widgets, system notifications, and Quick Settings remain controlled by Android/the device maker.

Weather values in this starter version are user-editable display values, not live forecast data. Some Control Center tiles are visual shortcuts; Wi-Fi/Bluetooth open Android's own settings/panels rather than silently changing protected system settings.

## Build an APK

The build machine used to prepare this source did not have the Android SDK or Gradle installed, and network access was unavailable, so no compiled APK could be honestly produced here. The project is ready for Android Studio or the included GitHub Actions workflow to compile.

### GitHub Actions (no local Android setup required)

1. Create a GitHub repository and upload the contents of this folder.
2. Open **Actions** and run **Build Aurora APK** (or push to `main`/`master`).
3. Open the completed workflow run and download the `Aurora-iOS-Launcher-debug-apk` artifact.
4. Extract it, then install `app-debug.apk` on your Android phone. Android may ask you to allow installation from the browser/file manager you use.
5. Press the phone's Home button and select **Aurora iOS Launcher**. Set it as the default Home app if prompted.

### Android Studio

Open this folder as an Android Studio project, let Android Studio install the required Android SDK packages and sync, then choose **Build → Build APK(s)**. The included `gradlew.bat` bootstrapper downloads Gradle 8.7 on Windows when needed (internet required). The generated file should be at `app/build/outputs/apk/debug/app-debug.apk`. If Android Studio says the Android SDK is missing, install Android SDK Platform 35 from **Tools → SDK Manager** first.

## Browser demo

Open `Aurora_iOS_Launcher_Demo.html` (provided beside the source ZIP) in a desktop browser to explore the visual editor. The browser version saves its configuration locally but cannot launch Android packages or change system wallpaper.

## Technical notes

- Java / Android WebView; no third-party runtime libraries.
- `minSdk 26`, `compileSdk 35`, `targetSdk 35`.
- Gradle 8.7 and Android Gradle Plugin 8.6.1.
- Personal settings and wallpapers are stored in the app WebView's local storage. Large photos are resized before saving.
