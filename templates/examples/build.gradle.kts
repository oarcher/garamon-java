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
    implementation("org.garamon.GENERIC:garamon-GENERIC:1.0")  // must match template/build.gradle.kts
}

application {
    mainClass.set("Main")
}

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
