# Developing Budgy

Notes for building, signing and working on Budgy. For what the app does, see the [README](../README.md).

## Open and run
1. Android Studio (Ladybug 2024.2 or newer) → **File → Open** → pick this `Budgy` folder.
2. Wait for Gradle sync to finish (first time downloads dependencies).
3. Pick a device or emulator (Android 8.0 / API 26 or newer) and press **Run ▶**.

## Build an APK to share
**Build → Build App Bundle(s) / APK(s) → Build APK(s)**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Smaller release APK: run `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
  (signed with the debug key so friends can install it directly; use your own keystore before any Play Store release)

## Sign releases with your own key (do this once, before sharing)
Release APKs are signed with the debug key unless you add your own. Your own key means updates always install
over the old version. If friends already have a debug-signed Budgy, they must back up, uninstall once,
install the new APK, then restore — after that, updates install normally.

1. Android Studio → **Build → Generate Signed App Bundle / APK → APK → Create new…**
   Save the key store as `budgy-release.jks` in this project folder, alias `budgy`. Write the passwords down.
2. Copy `keystore.properties.example` to `keystore.properties` and fill in the passwords.
3. Run `./gradlew assembleRelease`. The APK is at `app/build/outputs/apk/release/app-release.apk`.
4. Back up `budgy-release.jks` and `keystore.properties` somewhere safe (not in a public repo).
   Lose them and you can't ship updates that install over the old app.

## Project map
```
app/src/main/java/com/budgy/app/
  BudgyApp.kt, MainActivity.kt
  data/        Room entities, DAO, database, repository, DataStore settings, default categories
  notify/      ReminderWorker (WorkManager) + notification channel
  util/        Money formatting/parsing, dates, subscription cycle math, Invest (cost basis), CoinGecko client
  ui/          ViewModel, navigation root, theme, shared components, screens/
  i18n/        L10n: in-app translations (assets/i18n/fil.json, ceb.json)
  widget/      Home screen widget
```
