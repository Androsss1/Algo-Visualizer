import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4:1.8.2")
    testImplementation("org.jetbrains.compose.ui:ui-test:1.8.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("GuiAlgo")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "MainKt" }
    val runtimeClasspath by configurations
    from(runtimeClasspath.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as Jar)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GuiAlgo"
            packageVersion = "1.0.0"
        }
    }
}