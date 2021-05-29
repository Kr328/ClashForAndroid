private const val appCenterVersion = "4.1.1"
private const val serializationVersion = "1.2.1"
private const val lifecycleVersion = "2.3.1"
private const val roomVersion = "2.3.0"

object Libs {
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0"
    const val core = "androidx.core:core-ktx:1.5.0"
    const val appCompat = "androidx.appcompat:appcompat:1.3.0"
    const val activity = "androidx.activity:activity-ktx:1.2.3"
    const val fragment = "androidx.fragment:fragment-ktx:1.3.4"
    const val coordinatorLayout = "androidx.coordinatorlayout:coordinatorlayout:1.1.0"
    const val material = "com.google.android.material:material:1.3.0"
    const val recyclerView = "androidx.recyclerview:recyclerview:1.2.0"
    const val multiProcess = "dev.rikka.rikkax.preference:multiprocess:1.0.0"
    const val serializationJson =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"
    const val viewPager2 = "androidx.viewpager2:viewpager2:1.0.0"

    const val roomApt = "androidx.room:room-compiler:$roomVersion"

    val lifecycle = arrayOf(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion",
        "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion",
        "androidx.lifecycle:lifecycle-service:$lifecycleVersion",
        "androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion"
    )
    val room = arrayOf(
        "androidx.room:room-runtime:$roomVersion",
        "androidx.room:room-ktx:$roomVersion"
    )
    val appCenter = arrayOf(
        "com.microsoft.appcenter:appcenter-analytics:$appCenterVersion",
        "com.microsoft.appcenter:appcenter-crashes:$appCenterVersion"
    )
}
