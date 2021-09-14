import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    checkstyle
    pmd
    id("net.ltgt.errorprone") version "2.0.2"
    id("com.github.spotbugs") version "4.7.5"
    id("org.javamodularity.moduleplugin") version "1.8.9"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
    modularity.inferModulePath.set(false)
}

repositories {
    mavenCentral()
}

dependencies {
    // Checks
    errorprone("com.google.errorprone:error_prone_core:2.9.0")
    checkstyle("com.puppycrawl.tools:checkstyle:9.0")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")

    // Annotations for better code documentation
    implementation("org.jetbrains:annotations:22.0.0")
    implementation("com.github.spotbugs:spotbugs-annotations:4.4.1")

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
    compilerArgs.add("-Xlint:-requires-automatic")
}

// Error prone options
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}

checkstyle {
    tasks.checkstyleMain {
        configFile = file("checkstyle.xml")

        reports {
            xml.required.set(false)
            html.required.set(true)
        }
    }

    tasks.checkstyleTest {
        enabled = false
    }
}

pmd {
    isConsoleOutput = true
    toolVersion = "6.38.0"
    ruleSets = listOf()
    ruleSetConfig = project.resources.text.fromFile("pmd.xml")

    tasks.pmdMain {
        reports.xml.required.set(false)
    }

    tasks.pmdTest {
        enabled = false
    }
}

spotbugs {
    showProgress.set(true)

    tasks.spotbugsMain {
        reports.maybeCreate("html").isEnabled = true
    }

    tasks.spotbugsTest {
        enabled = false
    }
}
