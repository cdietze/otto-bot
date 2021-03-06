plugins {
    kotlin("jvm") version "1.5.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.register<JavaExec>("execute") {
    val mode = findProperty("mode")?.toString() ?: "Hello"
    println("Selected mode: $mode")
    main = "ottobot.${mode.toLowerCase()}.${mode}Kt"
    classpath = sourceSets.main.get().runtimeClasspath
}
