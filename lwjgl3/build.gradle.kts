plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by project

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.iagothz.mergetd.lwjgl3.Lwjgl3LauncherKt")
}
