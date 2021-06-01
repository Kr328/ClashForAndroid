## Clash for Android

A Graphical user interface of [clash](https://github.com/Dreamacro/clash) for Android

<a href="https://play.google.com/store/apps/details?id=com.github.kr328.clash"><img width="200px" alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"/></a> or [Releases](https://github.com/Kr328/ClashForAndroid/releases)

### Feature

Fully feature of [clash](https://github.com/Dreamacro/clash) ~~(Exclude `external-controller`~~

### Requirement

- Android 5.0+ (minimum)
- Android 7.0+ (recommend)
- `armeabi-v7a` , `arm64-v8a`, `x86` or `x86_64` Architecture

### License

See also [LICENSE](./LICENSE) and [NOTICE](./NOTICE)

### Privacy Policy

See also [PRIVACY_POLICY.md](./PRIVACY_POLICY.md)

### Build

1. Update submodules

   ```bash
   git submodule update --init --recursive
   ```

2. Install **OpenJDK 11**, **Android SDK**, **CMake** and **Golang**

3. Create `local.properties` in project root with

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. Create `keystore.properties` in project root with

   ```properties
   storeFile=/path/to/keystore/file
   storePassword=<key store password>
   keyAlias=<key alias>
   keyPassword=<key password>
   ```

5. Build

   ```bash
   ./gradlew app:assembleFossRelease
   ```

6. Pick `app-foss-<arch>-release-signed.apk` in `app/build/outputs/apk/foss/release/`
