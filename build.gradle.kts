import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    id("net.ltgt.errorprone") version "2.0.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Checks
    errorprone("com.google.errorprone:error_prone_core:2.7.1")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Annotations for better code documentation
    implementation("org.jetbrains:annotations:21.0.1")

    // Guava primitives
    implementation("com.google.guava:guava:30.1.1-jre")

    // JUnit Jupiter test framework
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}

tasks {
    test {
        maxHeapSize = "128m"
        useJUnitPlatform()
    }
}

application {
    // Define the main class for the application
    mainClass.set("ru.mail.polis.Client")

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx128m")
}

// Fail on warnings
tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Werror")
    compilerArgs.add("-Xlint:all")
}

// Error prone options
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}