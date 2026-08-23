import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun hasSigningConfig() = localPropertiesFile.exists()
        && localProperties.getProperty("SIGNING_KEY_PATH_OVERRIDE") != null

android {
    signingConfigs {
        getByName("debug") {
            if (hasSigningConfig()) {
                storeFile = file(localProperties.getProperty("SIGNING_KEY_PATH_OVERRIDE"))
                storePassword = localProperties.getProperty("SIGNING_KEY_PASSWORD")
                keyPassword = localProperties.getProperty("SIGNING_KEY_ALIAS_PASSWORD")
                keyAlias = localProperties.getProperty("SIGNING_KEY_ALIAS")
            }
        }
        create("release") {
            if (hasSigningConfig()) {
                storeFile = file(localProperties.getProperty("SIGNING_KEY_PATH_OVERRIDE"))
                storePassword = localProperties.getProperty("SIGNING_KEY_PASSWORD")
                keyPassword = localProperties.getProperty("SIGNING_KEY_ALIAS_PASSWORD")
                keyAlias = localProperties.getProperty("SIGNING_KEY_ALIAS")
            }
        }
    }

    namespace = "dev1503.browseevo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev1503.browseevo"
        minSdk = 21
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    // Source: https://mvnrepository.com/artifact/org.mozilla.geckoview/geckoview
    implementation("org.mozilla.geckoview:geckoview:144.0.20251027123126")
    implementation(project(":baseokhttpx"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.L-JINBIN:MTDataFilesProvider:v1.0.0")
    implementation("androidx.preference:preference:1.2.1")
}