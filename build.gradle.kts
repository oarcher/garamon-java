import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.file.Paths

plugins { java }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    sourceSets.main {
        java.srcDir(layout.buildDirectory.dir("generated"))
    }
}

tasks.withType<Jar> {
    from(java.sourceSets.main.get().output.classesDirs)
}


val libPath = providers.gradleProperty("lib")
val header  = providers.gradleProperty("header")

// Dossiers sous build/
val genRoot    = layout.buildDirectory.dir("generated")
val classesDir = layout.buildDirectory.dir("classes-generated")
val distDir    = layout.buildDirectory.dir("dist")
val libDir     = layout.buildDirectory.dir("lib")
val templates  = layout.projectDirectory.dir("templates")
val templatesGen = templates.dir("generator")
val templatesMain = templates.asFileTree.matching { include("*.java") }
val templatesSample = templates.asFileTree.matching { include("sample/*.java") }

// Copy external garamon lib to build/lib
val copyNativeLib = tasks.register<Copy>("copyNativeLib") {
    val lib = libPath.orNull ?: error("Missing -Plib=/abs/path/to/lib")
    from(lib)
    into(libDir)
    outputs.dir(libDir)

    // Inline libLogicalName logic and store as extra property
    val base = Paths.get(lib).fileName.toString()
    val noPrefix = if (base.startsWith("lib")) base.removePrefix("lib") else base
    val logicalName = noPrefix.substringBefore('.') // libcga5.so -> cga5 / cga5.dll -> cga5
    extensions.extraProperties["libLogicalName"] = logicalName
    extensions.extraProperties["libPathValue"] = lib
}

// jextract from Mvec.h to build/generated/org/garamon/<libName>
val runJextract = tasks.register<Exec>("runJextract") {
    dependsOn(copyNativeLib)
    val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
    val hdr = header.orNull ?: error("Missing -Pheader=...")
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    commandLine(
        "jextract",
        "-t", "org.garamon.$libName",
        "-l", libName,
        "--use-system-load-library",
        "--output", genRoot.get().asFile.absolutePath,
        hdr
    )

    outputs.dir(genRoot.map { it.dir("org/garamon/$libName") })
}

// The minimal garamon parser requires some minimal parsing (replace string "GENERIC" with libName, ie "c5ga")
val firstStageTemplates = tasks.register<Copy>("firstStageTemplates") {
    dependsOn(runJextract)
    val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    from(templatesGen)
    into(genRoot.map { it.dir("org/garamon/$libName") })
    includeEmptyDirs = false
    filter { line: String -> line.replace("GENERIC", libName) }

    inputs.dir(templatesGen)
    outputs.dir(genRoot.map { it.dir("org/garamon/$libName") })
}

// Compile the minimal garamon parser
val compileParser = tasks.register<JavaCompile>("compileParser") {
    dependsOn(firstStageTemplates)
    val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    source = fileTree(genRoot.map { it.dir("org/garamon/$libName") }) { include("**/*.java") }
    doFirst {
        val files = source.files.sortedBy { it.absolutePath }
        println("Source files (${files.size}):")
        files.forEach { println(it.absolutePath) }
    }
    destinationDirectory.set(classesDir)
    classpath = files()
    options.encoding = "UTF-8"
    sourceCompatibility = "25"
    targetCompatibility = "25"
}

// function to run the minimal garamon parser
fun garamonParser(
    task: JavaExec,
    libName: String,
    outputDir: File,
    templateFiles: List<File>
) {
    task.apply {
        mainClass.set("org.garamon.$libName.GaramonParser")
        classpath(classesDir)
        jvmArgs(
            "--enable-native-access=ALL-UNNAMED",
            "-Djava.library.path=${libDir.get().asFile.absolutePath}"
        )
        args(
            "-d", outputDir.absolutePath,
            *templateFiles.map { it.absolutePath }.toTypedArray()
        )
    }
}

// Generate code from template with garamonParser
val runMiniParserMain = tasks.register<JavaExec>("runMiniParserMain") {
    dependsOn(compileParser)
    val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    doFirst {
        println("templatesMain files (${templatesMain.files.size}):")
        templatesMain.files
            .sortedBy { it.absolutePath }
            .forEach { println(it.absolutePath) }
    }

    garamonParser(
        this,
        libName,
        genRoot.get().dir("org/garamon/$libName").asFile,
        templatesMain.files.toList()
    )
    // sortie: mêmes dossiers générés enrichis
    outputs.dir(genRoot.map { it.dir("org/garamon/$libName") })
}

// special case for Sample.java
// val generateSample = tasks.register<JavaExec>("generateSample") {
//     dependsOn(runMiniParserMain)
//     val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
//     val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
// 
//     garamonParser(
//         this,
//         libName,
//         distDir.get().asFile,
//         templatesSample.files.toList()
//     )
//     outputs.dir(distDir)
// }

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(java.sourceSets.main.get().allSource)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// Compile the whole project
val compileGeneratedFinal = tasks.register<JavaCompile>("compileGeneratedFinal") {
    dependsOn(runMiniParserMain)
    val lib = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    source = fileTree(genRoot.map { it.dir("org/garamon/$libName") }) { include("**/*.java") }
    destinationDirectory.set(classesDir)
    classpath = files()
    options.encoding = "UTF-8"
    sourceCompatibility = "25"
    targetCompatibility = "25"
}

// Check we are able to execute Sample

val runSample = tasks.register<JavaExec>("runSample") {
    dependsOn(compileGeneratedFinal)
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    mainClass.set("org.garamon.$libName.Sample")
    classpath(classesDir)
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Djava.library.path=${libDir.get().asFile.absolutePath}"
    )
}


val buildNative = tasks.register("buildNative") {
    group = "native"
    description = "jextract + preproc + compile + mini-parser + sample"
    dependsOn(runSample)
}

// tasks.named("build") {  // FIXME try build
//     dependsOn(runSample)
// }

tasks.named("assemble") {
    dependsOn(tasks.jar)
    dependsOn(sourcesJar)
    dependsOn(javadocJar)
}


