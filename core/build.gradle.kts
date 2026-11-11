plugins {
    kotlin("jvm")
}

val gdxVersion: String by project
val appVersion: String by project

val generateBuildInfo by tasks.registering {
    val version = appVersion
    val outDir = layout.buildDirectory.dir("generated/buildinfo")
    inputs.property("version", version)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/iagothz/mergetd/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.iagothz.mergetd
            |
            |object BuildInfo {
            |    const val VERSION = "$version"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generateBuildInfo)
}

ktlint {
    filter {
        exclude { it.file.path.contains("generated") }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
