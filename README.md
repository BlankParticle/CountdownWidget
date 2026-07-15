# Countdown Widget

A widget-only Android app — no launcher icon, no app UI. It puts a Material You
countdown widget on your home screen: a scalloped flower shape with a progress
ring that starts full and drains as the target date approaches, showing the
days left in the middle.

- Built with Jetpack Glance; colors come from the system's dynamic (wallpaper)
  theme and follow dark/light mode, matching stock Pixel widgets.
- All configuration happens in the widget flow: adding or tapping the widget
  opens a settings screen with a Material 3 date picker and an optional title
  that replaces the date line.
- Each widget instance has its own date/title, and a WorkManager job refreshes
  the count just after midnight.

## Building

Requires JDK 17+ and the Android SDK (API 36).

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

For a signed release build, create a `keystore.properties` in the project root:

```properties
storeFile=/path/to/your.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

then run `./gradlew assembleRelease`.

## License

[MIT](LICENSE). The bundled [Varela Round](https://fonts.google.com/specimen/Varela+Round)
font is licensed under the [SIL Open Font License](https://openfontlicense.org/).
