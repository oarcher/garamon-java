import org.gradle.api.tasks.bundling.Jar

plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.gradle:gradle-tooling-api:latest.integration")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("org.garamon.creator.App")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
