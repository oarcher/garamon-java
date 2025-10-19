plugins {
    application
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.garamon.c5ga:garamon-GENERIC:0.9")
}

application {
    mainClass.set("Main")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}


tasks.named<JavaExec>("run") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
