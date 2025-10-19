package org.garamon.project_namespace;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class NativeLoader {
    private static volatile boolean loaded;

    private NativeLoader() {}

    public static synchronized void load() {
        if (loaded) return;

        try {
            String baseName = readBaseNameFromProps("native-lib.properties", "generic");
            String classifier = System.getProperty("native.classifier", detectClassifier());
            String mappedName = System.mapLibraryName(baseName);
            String resourcePath = "/natives/" + classifier + "/" + mappedName;

            try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new RuntimeException("Native resource not found: " + resourcePath);
                }
                Path tmp = Files.createTempFile("nlib-", "-" + mappedName);
                tmp.toFile().deleteOnExit();
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(tmp.toAbsolutePath().toString());
            }

            loaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    private static String readBaseNameFromProps(String res, String fallback) {
        try (InputStream in = NativeLoader.class.getResourceAsStream("/" + res)) {
            if (in == null) return fallback;
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("native.baseName", fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String detectClassifier() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String normArch;
        if (arch.contains("aarch64") || arch.contains("arm64")) normArch = "aarch64";
        else if (arch.contains("64")) normArch = "x86_64";
        else if (arch.contains("86")) normArch = "x86";
        else normArch = arch;

        if (os.contains("win")) return "win-" + (normArch.equals("x86_64") ? "x64" : normArch);
        if (os.contains("mac") || os.contains("darwin")) return "macos-" + normArch;
        return "linux-" + normArch;
    }
}
