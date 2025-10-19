import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    `maven-publish`
}

group = "org.garamon.GENERIC"
version = "0.9"


repositories {
    mavenCentral()
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])   
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}


tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    val opts = options as StandardJavadocDocletOptions
    opts.addBooleanOption("Xdoclint:none", true)
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "garamon-GENERIC",
            "Implementation-Version" to (project.findProperty("version") ?: "0.1.0"),
            "Automatic-Module-Name" to "org.garamon.GENERIC"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    // Ensure native library resolution for tests
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("java.library.path", file("libs").absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}
