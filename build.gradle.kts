import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    checkstyle
    pmd
    id("net.ltgt.errorprone") version "4.1.0"
    id("com.github.spotbugs") version "6.0.27"
    id("org.javamodularity.moduleplugin") version "1.8.15"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
    modularity.inferModulePath.set(false)
}

repositories {
    mavenCentral()
}

dependencies {
    // Checks
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    checkstyle("com.puppycrawl.tools:checkstyle:10.21.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.15")

    // Annotations for better code documentation
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Popular data structures
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("it.unimi.dsi:fastutil:8.5.15")


    // JUnit Jupiter test framework
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
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
//    Disabled since we use JEP 383 which is currently in "incubator".
//    compilerArgs.add("-Werror")
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
    toolVersion = "7.8.0"
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
        reports.maybeCreate("html").required.set(true)
    }

    tasks.spotbugsTest {
        enabled = false
    }
}
