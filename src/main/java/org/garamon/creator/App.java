package org.garamon.creator;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    private void run(String[] args) throws Exception {
        Path libPath = null;
        Path mvecHPath = null;
        Path jextractPath = null;
        Path outDir = null;

        for (String arg : args) {
            if (arg.startsWith("--lib=")) {
                libPath = Path.of(arg.substring("--lib=".length()));
            } else if (arg.startsWith("--mvec_h=")) {
                mvecHPath = Path.of(arg.substring("--mvec_h=".length()));
            } else if (arg.startsWith("--jextract=")) {
                jextractPath = Path.of(arg.substring("--jextract=".length()));
            } else if (arg.startsWith("--out=")) {
                outDir = Path.of(arg.substring("--out=".length()));
            }
        }

        if (libPath == null || mvecHPath == null) {
            printUsageAndExit();
        }

        // Find jextract
        if (jextractPath == null) {
            jextractPath = findExecutable("jextract");
        }
        if (jextractPath == null || !Files.isExecutable(jextractPath)) {
            System.err.println(
                    "Error: jextract command not found. Please provide --jextract=<path> or ensure jextract is in your PATH.");
            System.exit(1);
        }

        // Define variables
        String libFileName = libPath.getFileName().toString();
        String noPrefix = libFileName.startsWith("lib") ? libFileName.substring(3) : libFileName;
        String libLogicalName = noPrefix.substring(0, noPrefix.indexOf('.'));
        Path algebraDir = Path.of("build-algebra");
        
        Path algebraSrc = algebraDir.resolve("src");
        Path resourcesDir = algebraSrc.resolve("main/resources");
        Path nativeDir = resourcesDir.resolve("natives");
        Path templatesDir = Path.of("templates");
        Path staticDir = Path.of("static");
        String algebraPkgName = "org.garamon." + libLogicalName;
        Path algebraPkgDir = algebraSrc.resolve("main/java/org/garamon/" + libLogicalName);
        Path tmpClassesDir = Files.createTempDirectory("garamon-classes");

        // Prepare algebra skeleton
        System.out.println("Preparing algebra skeleton in " + algebraDir);
        Files.createDirectories(algebraDir);
        copyDirectory(staticDir, algebraDir);

        Files.walk(templatesDir)
                .filter(Files::isRegularFile)
                .forEach(source -> {
                    try {
                        Path relative = templatesDir.relativize(source);
                        String replaced = relative.toString().replace("GENERIC", libLogicalName);
                        Path destination = algebraDir.resolve(replaced);
                        Files.createDirectories(destination.getParent());
                        String content = Files.readString(source).replace("GENERIC", libLogicalName);
                        Files.writeString(destination, content);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        // Copy native library
        System.out.println("Copying native library and writing properties");
        String os = System.getProperty("os.name").toLowerCase();
        String nativeClassifier;
        if (os.contains("win")) {
            nativeClassifier = "win-x64";
        } else if (os.contains("mac")) {
            nativeClassifier = "macos-aarch64";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            nativeClassifier = "linux-x86_64";
        } else {
            nativeClassifier = "unknown";
        }
        Path targetNativeDir = nativeDir.resolve(nativeClassifier);
        Files.createDirectories(targetNativeDir);
        Path targetLibPath = targetNativeDir.resolve(libFileName);
        Files.copy(libPath, targetLibPath, StandardCopyOption.REPLACE_EXISTING);

        Path propsFile = resourcesDir.resolve("native-lib.properties");
        String propsContent = Files.readString(propsFile);
        Files.writeString(propsFile, propsContent.replace("UNKNOWN", nativeClassifier));

        System.out.println("Algebra prepared at: " + algebraDir);

        // Run jextract
        System.out.println("Running jextract");
        execute(jextractPath.toString(),
                "-t", algebraPkgName,
                "-l", ":" + targetLibPath.toAbsolutePath(),
                "--output", algebraSrc.resolve("main/java").toAbsolutePath().toString(),
                mvecHPath.toAbsolutePath().toString());

        // Compile the minimal parser
        System.out.println("Compiling the minimal parser");
        execute("javac",
                "-d", tmpClassesDir.toString(),
                algebraPkgDir.resolve("GaramonParser.java").toString(),
                algebraPkgDir.resolve("Mvec_h.java").toString());

        // Run parser for java sources
        System.out.println("Running parser for java sources");
        List<String> javaFiles = Files.walk(algebraDir)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals("Mvec_h.java"))
                .map(Path::toString)
                .collect(Collectors.toList());

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-cp");
        command.add(tmpClassesDir.toString());
        command.add("-Djava.library.path=" + targetNativeDir.toAbsolutePath());
        command.add(algebraPkgName + ".GaramonParser");
        command.add("--inplace");
        command.addAll(javaFiles);
        execute(command);

        // Build, test, and publish
        if (outDir != null) {
            System.out.println("Copying examples to " + outDir.toAbsolutePath());
            try {
                copyDirectory(algebraDir.resolve("examples"), outDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        buildTestPublish(algebraDir);

        // Cleanup
        Files.walk(tmpClassesDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        // Final message
        System.out.println("\n--------------------------------------------------");
        System.out.println("SUCCESS!");
        System.out.println("Algebra project generated at: " + algebraDir.toAbsolutePath());
        Path finalExamplesDir;
        if (outDir != null) {
            finalExamplesDir = outDir;
        } else {
            finalExamplesDir = algebraDir;
        }
        System.out.println("Projetc skeleton available at: " + finalExamplesDir.toAbsolutePath().normalize());
        System.out.println("--------------------------------------------------");
    }

    private void execute(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        runProcess(pb);
    }

    private void execute(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        runProcess(pb);
    }

    static void runGradleTasks(Path projectDir, String... tasks) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile());
        try (ProjectConnection connection = connector.connect()) {
            BuildLauncher build = connection.newBuild().forTasks(tasks);
            build.setStandardOutput(System.out);
            build.setStandardError(System.err);
            build.run();
        }
    }

    static void buildTestPublish(Path algebraDir) {
        System.out.println("Building final package");
        runGradleTasks(algebraDir, "build");
        System.out.println("Testing final package");
        runGradleTasks(algebraDir, "test");
        System.out.println("Publishing to MavenLocal");
        runGradleTasks(algebraDir, "publishToMavenLocal");
    }

    private void runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Command failed with exit code " + exitCode);
            System.exit(exitCode);
        }
    }

    private Path findExecutable(String name) {
        return Pattern.compile(System.getProperty("path.separator"))
                .splitAsStream(System.getenv("PATH"))
                .map(Paths::get)
                .map(p -> p.resolve(name))
                .filter(Files::isExecutable)
                .findFirst()
                .orElse(null);
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = destination.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void printUsageAndExit() {
        System.err.println(
                "Usage: java -jar create-package.jar --lib=<path> --mvec_h=<path> [--jextract=<path>] [--out=<path>]");
        System.err.println();
        System.err.println("This script generates Java bindings for a Geometric Algebra library generated by Garamon.");
        System.err.println("Garamon is a C++ library generator for Geometric Algebra.");
        System.err.println();
        System.err.println("Arguments:");
        System.err.println("  --lib=<path>      Path to the native library (e.g., libc5ga.so, c5ga.dll).");
        System.err.println("  --mvec_h=<path>   Path to the C header file (e.g., Mvec.h) for jextract.");
        System.err.println(
                "  --jextract=<path> Path to the jextract tool executable (optional). If not provided, jextract must be in PATH.");
        System.err.println(
                "  --out=<path>      Directory where the 'examples' project will be copied (optional).");
        System.exit(1);
    }
}