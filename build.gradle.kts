import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins { java }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

val algebraProp = providers.gradleProperty("algebra")

data class AlgebraResolved(
    val name: String,
    val libFile: File?,
    val headerFile: File?
)

fun runCommand(cmd: List<String>, cwd: File) {
    val pb = ProcessBuilder(cmd)
    pb.directory(cwd)
    pb.inheritIO()
    val p = pb.start()
    val code = p.waitFor()
    if (code != 0) throw GradleException("Command failed (${cmd.joinToString(" ")}), exit=$code")
}

fun resolveAlgebra(projectDir: File, algebraInput: String): AlgebraResolved {
    val root = project.file(algebraInput).absoluteFile
    require(root.exists() && root.isDirectory) { "Algebra dir not found: $root" }

    val base = root.name
    val name = if (base.startsWith("garamon_") && base.length > "garamon_".length) {
        base.removePrefix("garamon_")
    } else {
        error("Cannot infer algebra name from $root")
    }

    val libCandidates = listOf(
        File(root, "lib$name.so"),
        File(root, "lib$name.dylib"),
        File(root, "$name.dll")
    ) + (root.listFiles()?.filter { it.isFile && it.name.matches(Regex("""lib.*\.(so|dylib|dll)$""")) } ?: emptyList())
    val libFile = libCandidates.firstOrNull { it.isFile }

    val header = File(root, "src/$name/Mvec.h")
    val headerFile = header.takeIf { it.isFile }

    return AlgebraResolved(name, libFile, headerFile)
}

val libPath: Provider<String> = providers.gradleProperty("lib").orElse(
    algebraProp.map { a ->
        val r = resolveAlgebra(project.projectDir, a)
        r.libFile?.absolutePath ?: error("Cannot locate native library under $a")
    }
)

val header: Provider<String> = providers.gradleProperty("header").orElse(
    algebraProp.map { a ->
        val r = resolveAlgebra(project.projectDir, a)
        r.headerFile?.absolutePath ?: error("Cannot locate header under $a (expected src/${r.name}/Mvec.h)")
    }
)

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
    val lib = libPath.orNull ?: error("Missing -Palgebra=...")

    val nativeClassifier: String = when {
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "win-x64"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos-aarch64"
        else -> "linux-x86_64"
    }

    val targetNativeDir = nativeDir.map { it.dir(nativeClassifier) }

    from(lib)
    into(targetNativeDir)
    outputs.dir(targetNativeDir)

    val base = Paths.get(lib).fileName.toString()
    val noPrefix = if (base.startsWith("lib")) base.removePrefix("lib") else base
    val logicalName = noPrefix.substringBefore('.')
    extensions.extraProperties["libLogicalName"] = logicalName
    extensions.extraProperties["libPathValue"] = lib

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
    val hdr = header.orNull ?: error("Missing -Palgebra=...")
    val libName = copyNativeLib.get().extensions.extraProperties["libLogicalName"] as String

    commandLine(
        "jextract",
        "-t", "org.garamon.$libName",
        "-l", ":${project.file(libPath.get()).absolutePath}",
        "--output", algebraSrcMain.get().asFile.absolutePath,
        hdr
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
tasks.register("makeAlgebra") {
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
val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(java.sourceSets.main.get().allSource)
}
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}
tasks.named("assemble") {
    dependsOn(tasks.jar)
    dependsOn(sourcesJar)
    dependsOn(javadocJar)
}
