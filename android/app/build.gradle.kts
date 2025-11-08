import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader(Charsets.UTF_8).use { reader ->
        localProperties.load(reader)
    }
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { inputStream ->
        keystoreProperties.load(inputStream)
    }
}

android {
    namespace = "com.gokadzev.musify"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    defaultConfig {
        applicationId = "com.gokadzev.musify"
        minSdk = 24
        targetSdk = 36
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        // Add build config fields for method channel
        buildConfigField("String", "METHOD_CHANNEL", "\"com.gokadzev.musify/widget\"")
    }

    flavorDimensions += "flavor"

    productFlavors {
        create("github") {
            dimension = "flavor"
            applicationIdSuffix = ""
            // Override for github flavor
            buildConfigField("String", "METHOD_CHANNEL", "\"com.gokadzev.musify/widget\"")
            // Override for fdroid flavor
            buildConfigField("String", "METHOD_CHANNEL", "\"com.gokadzev.musify.fdroid/widget\"")
        }
        create("fdroid") {
            dimension = "flavor"
            applicationIdSuffix = ".fdroid"
        }
    }

    signingConfigs {
        create("release") {
            // From decoded key
            storeFile = file("key.jks")

            // From key.properties
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }

    buildFeatures {
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = false
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " DEBUG"
            // For debug builds, use the main package name
            buildConfigField("String", "METHOD_CHANNEL", "\"com.gokadzev.musify.debug/widget\"")
        }
    }
}

flutter {
    source = "../.."
}
