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
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

sourceSets.main {
    resources.srcDir("../assets")
}

application {
    mainClass.set("com.iagothz.mergetd.lwjgl3.Lwjgl3LauncherKt")
}
