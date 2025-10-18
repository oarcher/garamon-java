import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins { java }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

val libFileProvider: Provider<File> = providers.gradleProperty("lib").map { project.file(it) }

val headerFileProvider: Provider<File> = providers.gradleProperty("mvec_h").map { project.file(it) }

val jextractPathProvider: Provider<File> = providers.gradleProperty("jextract").map { project.file(it) }

fun runCommand(cmd: List<String>, cwd: File) {
    val pb = ProcessBuilder(cmd)
    pb.directory(cwd)
    pb.inheritIO()
    val p = pb.start()
    val code = p.waitFor()
    if (code != 0) throw GradleException("Command failed (${cmd.joinToString(" ")}), exit=$code")
}

// Build dirs
val algebraDir      = layout.buildDirectory.dir("algebra")
val algebraSrcMain  = algebraDir.map { it.dir("src/main/java") }
val algebraSrcTest  = algebraDir.map { it.dir("src/test/java") }
val classesDir      = algebraDir.map { it.dir("tmp/classes") }
val resourcesDir    = algebraDir.map { it.dir("src/main/resources" ) }
val nativeDir       = resourcesDir.map { it.dir("natives") }
val templates    = layout.projectDirectory.dir("templates")
val templatesGen = templates.dir("generator")
val templatesMain  = templates.asFileTree.matching { include("*.java") }
val templatesTests = templates.asFileTree.matching { include("tests/*.java") }

// Copy external garamon lib to build/lib and expose libLogicalName
val copyNativeLib = tasks.register<Copy>("copyNativeLib") {
    val lib = libFileProvider.orNull ?: error("Missing -Plib=...")

    val nativeClassifier: String = when {
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "win-x64"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos-aarch64"
        else -> "linux-x86_64"
    }

    val targetNativeDir = nativeDir.map { it.dir(nativeClassifier) }

    from(lib)
    into(targetNativeDir)
    outputs.dir(targetNativeDir)

    val base = lib.name
    val noPrefix = if (base.startsWith("lib")) base.removePrefix("lib") else base
    val logicalName = noPrefix.substringBefore('.')
    extensions.extraProperties["libLogicalName"] = logicalName
    extensions.extraProperties["libPathValue"] = lib.absolutePath

    doLast {
        val propertiesFile = resourcesDir.get().asFile.resolve("native-lib.properties")
        propertiesFile.writeText("""
            native.baseName=${logicalName}
            native.classifier.default=${nativeClassifier}

        """.trimIndent())
    }
}

// Prepare algebra skeleton (dirs + templates + native lib + wrapper)
val prepareAlgebra = tasks.register("prepareAlgebra") {
    dependsOn(copyNativeLib)
    doLast {
        val libLogicalName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
        val dir = algebraDir.get().asFile
        val pkg = "org.garamon.$libLogicalName"

        val srcMain = algebraSrcMain.get().asFile
        val srcTest = algebraSrcTest.get().asFile
        val libs    = dir.resolve("libs")
        listOf(srcMain, srcTest, libs).forEach { it.mkdirs() }

        val subs = mapOf("NAME" to libLogicalName, "PKG" to pkg)
        fun render(templateRelPath: String, subs: Map<String, String>): String {
            val f = templates.file(templateRelPath).asFile
            require(f.exists()) { "Missing template: $templateRelPath" }
            var txt = f.readText()
            subs.forEach { (k, v) -> txt = txt.replace("\${$k}", v) }
            return txt
        }
        dir.resolve("build.gradle.kts").writeText(render("algebra/build.gradle.kts.in", subs))
        dir.resolve("settings.gradle.kts").writeText(render("algebra/settings.gradle.kts.in", subs))

        val libPathValue = copyNativeLib.get().extensions.extraProperties["libPathValue"] as String
        val libFile = File(libPathValue)
        if (libFile.isFile) {
            Files.copy(
                libFile.toPath(),
                libs.resolve(libFile.name).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        val wrapperTpl = templates.dir("wrapper").asFile
        require(wrapperTpl.exists()) { "Missing Gradle wrapper template at templates/wrapper/" }
        listOf("gradlew", "gradlew.bat").forEach { n ->
            val src = File(wrapperTpl, n)
            if (src.isFile) {
                val dst = File(dir, n)
                dst.parentFile.mkdirs()
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
                if (n == "gradlew") dst.setExecutable(true)
            }
        }
        val wrapSrcDir = File(wrapperTpl, "gradle/wrapper")
        if (wrapSrcDir.isDirectory) {
            val wrapDstDir = File(dir, "gradle/wrapper")
            wrapDstDir.mkdirs()
            wrapSrcDir.listFiles()?.forEach { f ->
                Files.copy(f.toPath(), File(wrapDstDir, f.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        println("Algebra prepared at: ${dir.absolutePath}")
    }

}

// jextract outputs directly into algebra/src/main/java
val runJextract = tasks.register<Exec>("runJextract") {
    dependsOn(prepareAlgebra)
    val jextractExe = jextractPathProvider.orNull ?: error("Missing -Pjextract=... (path to jextract.bat)")
    require(jextractExe.isFile) { "jextract executable not found: $jextractExe" }
    val hdr = headerFileProvider.orNull ?: error("Missing -Pmevc_h=...")
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    commandLine(
        jextractExe.absolutePath,
        "-t", "org.garamon.$libName",
        "-l", ":${libFileProvider.get().absolutePath}",
        "--output", algebraSrcMain.get().asFile.absolutePath,
        hdr.absolutePath
    )
    outputs.dir(algebraSrcMain.map { it.dir("org/garamon/$libName") })
}

// First-stage generator templates into algebra/src/main/java (replace GENERIC)
val firstStageTemplates = tasks.register<Copy>("firstStageTemplates") {
    dependsOn(runJextract)
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    doFirst {
        val genDir = templatesGen.asFile
        val genFiles = genDir.listFiles()?.sortedBy { it.absolutePath } ?: emptyList()
        println("templatesGen files (${genFiles.size}):")
        genFiles.forEach { println(it.absolutePath) }
    }
    from(templatesGen)
    into(algebraSrcMain.map { it.dir("org/garamon/$libName") })
    includeEmptyDirs = false
    filter { line: String -> line.replace("GENERIC", libName) }

    inputs.dir(templatesGen)
    outputs.dir(algebraSrcMain.map { it.dir("org/garamon/$libName") })
}

// Compile the minimal parser (sources are now in algebra/src/main/java)
val compileParser = tasks.register<JavaCompile>("compileParser") {
    dependsOn(firstStageTemplates)
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String
    source = fileTree(algebraSrcMain.map { it.dir("org/garamon/$libName") }) { include("**/*.java") }
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

// Utility to run the minimal garamon parser
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
            "-Djava.library.path=${nativeDir.get().asFile.absolutePath}"
        )
        args(
            "-d", outputDir.absolutePath,
            *templateFiles.map { it.absolutePath }.toTypedArray()
        )
    }
}

// Generate main sources with garamonParser into algebra/src/main/java
val runMiniParserMain = tasks.register<JavaExec>("runMiniParserMain") {
    dependsOn(compileParser)
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    doFirst {
        println("templatesMain files (${templatesMain.files.size}):")
        templatesMain.files.sortedBy { it.absolutePath }.forEach { println(it.absolutePath) }
    }

    garamonParser(
        this,
        libName,
        algebraSrcMain.get().dir("org/garamon/$libName").asFile,
        templatesMain.files.toList()
    )
    outputs.dir(algebraSrcMain.map { it.dir("org/garamon/$libName") })
}

// Generate test sources with garamonParser into algebra/src/test/java
val runMiniParserTests = tasks.register<JavaExec>("runMiniParserTests") {
    dependsOn(compileParser)
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    doFirst {
        println("templatesTests files (${templatesTests.files.size}):")
        templatesTests.files.sortedBy { it.absolutePath }.forEach { println(it.absolutePath) }
    }

    garamonParser(
        this,
        libName,
        algebraSrcTest.get().dir("org/garamon/$libName").asFile,
        templatesTests.files.toList()
    )
    outputs.dir(algebraSrcTest.map { it.dir("org/garamon/$libName") })
}

// Build the algebra with its own wrapper
val makeAlgebra = tasks.register("makeAlgebra") {
    group = "algebra"
    description = "Generate the algebra under build/algebra and build it with its wrapper"

    dependsOn(runMiniParserMain, runMiniParserTests)

    doLast {
        val dir = algebraDir.get().asFile
        println("Algebra emitted at: ${dir.absolutePath}")

        println("To build java package: (cd build/algebra ; ./gradlew build)")
    }
}

// Keep root artifacts if needed (sources/javadoc for the tiny parser)
tasks.named("assemble") {
    dependsOn(makeAlgebra)
}

