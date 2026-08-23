plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kongzue.baseokhttp.x"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")
    implementation("com.github.kongzue:BaseJson:1.2.8")
}
