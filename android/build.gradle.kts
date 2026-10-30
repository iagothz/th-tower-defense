import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by project
val natives: Configuration by configurations.creating

// Release signing comes from android/keystore.properties (not in git); without it, release builds are unsigned.
val keystoreProps = Properties().apply {
    val f = rootProject.file("android/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.iagothz.mergetd"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iagothz.mergetd"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0"
    }

    sourceSets["main"].apply {
        manifest.srcFile("AndroidManifest.xml")
        java.srcDirs("src/main/kotlin")
        res.srcDirs("res")
        jniLibs.srcDirs("libs")
        assets.srcDirs("../assets")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 with the libGDX keep rules in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    // Google Play Games Services v2: sign-in and Saved Games for the cloud save.
    implementation("com.google.android.gms:play-services-games-v2:21.0.0")
    listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").forEach {
        natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$it")
        natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-$it")
    }
}

// Extracts the libGDX native .so files into libs/<abi>/ before packaging.
val copyAndroidNatives by tasks.registering {
    doFirst {
        natives.files.forEach { jar ->
            val abi = jar.name.substringAfter("natives-").substringBefore(".jar")
            val outDir = file("libs/$abi")
            outDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outDir)
                include("*.so")
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn(copyAndroidNatives)
}
