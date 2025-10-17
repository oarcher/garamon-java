package org.garamon.GENERIC;

public final class NativeLoader {
    private static volatile boolean loaded;

    private NativeLoader() {}

    public static synchronized void load() {
        if (!loaded) {
            /// System.load(absolutePathToLib);
            // System.loadLibrary("GENERIC");
            loaded = true;
        }
    }
}
