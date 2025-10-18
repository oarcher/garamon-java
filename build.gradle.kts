import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.util.Properties

class PrefixedOutputStream(
    private val prefix: String,
    private val delegate: OutputStream
) : OutputStream() {
    private val lineBuffer = ByteArrayOutputStream()

    override fun write(b: Int) {
        if (b == '\n'.code) {
            delegate.write(prefix.toByteArray())
            delegate.write(lineBuffer.toByteArray())
            delegate.write('\n'.code)
            lineBuffer.reset()
        } else {
            lineBuffer.write(b)
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        flush()
        delegate.close()
    }
}

 fun calculateLogicalName(libFile: File): String {
    val base = libFile.name
    val noPrefix = if (base.startsWith("lib")) base.removePrefix("lib") else base
    return noPrefix.substringBefore('.')
 }
 
 plugins { java }
 
 // Load optional algebra properties
val loadedProps = Properties()
val confFileProvider: Provider<File> = getPropertyProvider("conf").map { layout.projectDirectory.file(it).asFile }

if (confFileProvider.isPresent) {
    val confFile = confFileProvider.get()
    if (confFile.exists()) {
        confFile.inputStream().use { loadedProps.load(it) }
    }
}
 
 fun getPropertyProvider(name: String): Provider<String> {
    return providers.provider {
        loadedProps.getProperty(name)
            ?.trim()
            ?.trim('"')
            ?.replace("\\", "/")
            ?: providers.gradleProperty(name).orNull
    }
 }
 tasks.register("checkConf") {
    doLast {
        val missingProps = mutableListOf<String>()

        if (!confFileProvider.isPresent) {
            missingProps.add("conf")
        } else {
            val confFile = confFileProvider.get()
            if (!confFile.exists()) {
                throw GradleException("Configuration file not found: '${confFile.absolutePath}'. Please ensure the file exists and is accessible.")
            }
            if (loadedProps.isEmpty) {
                throw GradleException("Configuration file '${confFile.absolutePath}' is empty or could not be loaded. Please ensure it contains 'lib', 'mvec_h', and 'jextract' properties.")
            }
            if (!libFileProvider.isPresent) {
                missingProps.add("lib")
            }
            if (!headerFileProvider.isPresent) {
                missingProps.add("mvec_h")
            }
            if (!jextractPathProvider.isPresent) {
                missingProps.add("jextract")
            }
        }

        if (missingProps.isNotEmpty()) {
            val message = """
                Missing required configuration properties: ${missingProps.joinToString(", ")}.
                
                Please ensure the 'conf' property is defined via -Pconf=path/to/algebra.conf and your algebra.conf contains:
                
                lib=path/to/your/library.so
                mvec_h=path/to/your/Mvec_h.h
                jextract=path/to/your/jextract_executable
            """.trimIndent()
            throw GradleException(message)
        }
    }
 }
 
 java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
 }
 
 tasks.jar { enabled = false }
 
 val libFileProvider: Provider<File> = getPropertyProvider("lib").map { layout.projectDirectory.file(it).asFile }
 val libLogicalNameProvider: Provider<String> = libFileProvider.map { calculateLogicalName(it) }
 
 val headerFileProvider: Provider<File> = getPropertyProvider("mvec_h").map { layout.projectDirectory.file(it).asFile }
 val jextractPathProvider: Provider<File> = getPropertyProvider("jextract").map { layout.projectDirectory.file(it).asFile }
 
 fun runCommand(cmd: List<String>, cwd: File) {
    val pb = ProcessBuilder(cmd)
    pb.directory(cwd)
    pb.inheritIO()
    val p = pb.start()
    val code = p.waitFor()
    if (code != 0) throw GradleException("Command failed (${cmd.joinToString(" ")}), exit=$code")
 }
 
 val algebraDir      = layout.buildDirectory.dir("algebra")
 val algebraSrcMain  = algebraDir.map { it.dir("src/main/java") }
 val algebraSrcTest  = algebraDir.map { it.dir("src/test/java") }
 val classesDir      = algebraDir.map { it.dir("tmp/classes") }
 val resourcesDir    = algebraDir.map { it.dir("src/main/resources" ) }
 val nativeDir       = resourcesDir.map { it.dir("natives") }
 val templates       = layout.projectDirectory.dir("templates")
 val templatesGen    = templates.dir("generator")
 val templatesMain   = templates.asFileTree.matching { include("*.java") }
 val templatesTests  = templates.asFileTree.matching { include("tests/*.java") }
 
 val copyNativeLib = tasks.register<Copy>("copyNativeLib") {
    dependsOn("checkConf")
    val nativeClassifier = when {
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "win-x64"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos-aarch64"
        else -> "linux-x86_64"
    }
    val targetNativeDir = nativeDir.map { it.dir(nativeClassifier) }
 
    onlyIf { libFileProvider.isPresent }
    into(targetNativeDir)
    outputs.dir(targetNativeDir)
    inputs.file(libFileProvider)
    from(libFileProvider)
    doFirst {
        // The 'from' declaration is now outside doFirst for proper up-to-date checks.
        // This block can be removed if no other doFirst logic is needed.
    }

 }
 
 val generateNativeLibProperties = tasks.register("generateNativeLibProperties") {
    dependsOn("checkConf")
    onlyIf { libFileProvider.isPresent }
    outputs.file(resourcesDir.map { it.file("native-lib.properties") })
    doLast {
        val nativeClassifier = when {
            org.gradle.internal.os.OperatingSystem.current().isWindows -> "win-x64"
            org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "macos-aarch64"
            else -> "linux-x86_64"
        }
        val propertiesFile = resourcesDir.get().asFile.resolve("native-lib.properties")
        propertiesFile.writeText(
            """
            native.baseName=${libLogicalNameProvider.get()}
            native.classifier.default=$nativeClassifier
            """.trimIndent()
        )
    }
 }
 
 val prepareAlgebra = tasks.register("prepareAlgebra") {
    dependsOn(copyNativeLib, generateNativeLibProperties)
    doLast {
        val libFile = libFileProvider.get()
        val dir = algebraDir.get().asFile
        val libLogicalName = libLogicalNameProvider.get()
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
    }
 }
 
 val runJextract = tasks.register<Exec>("runJextract") {
    dependsOn("checkConf", prepareAlgebra)
    doFirst {
        val jextractExe = jextractPathProvider.get()
        require(jextractExe.isFile) { "jextract executable not found: $jextractExe" }
        val hdr = headerFileProvider.get()
        val lib = libFileProvider.get()
        commandLine(
            jextractExe.absolutePath,
            "-t", "org.garamon.${libLogicalNameProvider.get()}",
            "-l", ":${lib.absolutePath}",
            "--output", algebraSrcMain.get().asFile.absolutePath,
            hdr.absolutePath
        )
    }
    outputs.dir(algebraSrcMain.zip(libLogicalNameProvider) { dir, name -> dir.dir("org/garamon/$name") })
 }
 
 val firstStageTemplates = tasks.register<Copy>("firstStageTemplates") {
    dependsOn(runJextract)
    from(templatesGen)
    val targetDir = algebraSrcMain.zip(libLogicalNameProvider) { dir, name -> dir.dir("org/garamon/$name") }
    into(targetDir)
    includeEmptyDirs = false
    filter { line: String -> line.replace("GENERIC", libLogicalNameProvider.get()) }
    inputs.dir(templatesGen)
    outputs.dir(targetDir)
 }
 
 val compileParser = tasks.register<JavaCompile>("compileParser") {
    dependsOn(firstStageTemplates)
    val srcDirProv = algebraSrcMain.zip(libLogicalNameProvider) { dir, name -> dir.dir("org/garamon/$name") }
    source = fileTree(srcDirProv) { include("**/*.java") }
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
 
 fun garamonParser(
    task: JavaExec,
    outputDir: File,
    templateFiles: List<File>
 ) {
    task.apply {
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
 
 val runMiniParserMain = tasks.register<JavaExec>("runMiniParserMain") {
    dependsOn(compileParser)
    mainClass.set(libLogicalNameProvider.map { "org.garamon.$it.GaramonParser" })
    val self = this
    doFirst {
        println("templatesMain files (${templatesMain.files.size}):")
        templatesMain.files.sortedBy { it.absolutePath }.forEach { println(it.absolutePath) }
        garamonParser(
            self,
            algebraSrcMain.get().dir("org/garamon/${libLogicalNameProvider.get()}").asFile,
            templatesMain.files.toList()
        )
    }
    outputs.dir(algebraSrcMain.zip(libLogicalNameProvider) { dir, name -> dir.dir("org/garamon/$name") })
 }
 
 val runMiniParserTests = tasks.register<JavaExec>("runMiniParserTests") {
    dependsOn(compileParser)
    mainClass.set(libLogicalNameProvider.map { "org.garamon.$it.GaramonParser" })
    val self = this
    doFirst {
        println("templatesTests files (${templatesTests.files.size}):")
        templatesTests.files.sortedBy { it.absolutePath }.forEach { println(it.absolutePath) }
        garamonParser(
            self,
            algebraSrcTest.get().dir("org/garamon/${libLogicalNameProvider.get()}").asFile,
            templatesTests.files.toList()
        )
    }
    outputs.dir(algebraSrcTest.zip(libLogicalNameProvider) { dir, name -> dir.dir("org/garamon/$name") })
 }
 
 val makeAlgebra = tasks.register("makeAlgebra") {
    group = "algebra"
    description = "Generate the algebra under build/algebra and build it with its wrapper"
    dependsOn(runMiniParserMain, runMiniParserTests)
    doLast {
        val dir = algebraDir.get().asFile
        println("""
            Algebra generated, but not yet built.
            To build it:

                cd ${dir}
                ./gradlew build
        """.trimIndent())
    }
 }
 

