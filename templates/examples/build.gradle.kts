plugins {
    application
}

import org.gradle.jvm.toolchain.JavaLanguageVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.garamon.GENERIC:garamon-GENERIC:0.9")
}

application {
    mainClass.set("Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
