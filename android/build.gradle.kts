plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by project
val natives: Configuration by configurations.creating

android {
    namespace = "com.iagothz.mergetd"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iagothz.mergetd"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets["main"].apply {
        manifest.srcFile("AndroidManifest.xml")
        java.srcDirs("src/main/kotlin")
        res.srcDirs("res")
        jniLibs.srcDirs("libs")
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
    listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").forEach {
        natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$it")
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
