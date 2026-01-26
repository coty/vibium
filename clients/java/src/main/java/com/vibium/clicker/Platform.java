package com.vibium.clicker;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Platform detection utilities for resolving clicker binaries.
 */
public class Platform {

    /**
     * Supported operating systems.
     */
    public enum OS {
        LINUX("linux"),
        DARWIN("darwin"),
        WINDOWS("win32");

        private final String identifier;

        OS(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Get the string identifier used in binary paths.
         *
         * @return Platform identifier (e.g., "linux", "darwin", "win32")
         */
        public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * Supported architectures.
     */
    public enum Arch {
        X64("x64"),
        ARM64("arm64");

        private final String identifier;

        Arch(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Get the string identifier used in binary paths.
         *
         * @return Architecture identifier (e.g., "x64", "arm64")
         */
        public String getIdentifier() {
            return identifier;
        }
    }

    private Platform() {
        // Static utility class
    }

    /**
     * Get the current operating system.
     *
     * @return The detected operating system
     * @throws UnsupportedOperationException If the OS is not supported
     */
    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return OS.LINUX;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return OS.DARWIN;
        } else if (os.contains("win")) {
            return OS.WINDOWS;
        }
        throw new UnsupportedOperationException("Unsupported operating system: " + os);
    }

    /**
     * Get the current architecture.
     *
     * @return The detected architecture
     * @throws UnsupportedOperationException If the architecture is not supported
     */
    public static Arch getArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return Arch.X64;
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return Arch.ARM64;
        }
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }

    /**
     * Get platform identifier for binary paths (e.g., "darwin-arm64").
     *
     * @return Platform identifier string
     */
    public static String getPlatformIdentifier() {
        return getOS().getIdentifier() + "-" + getArch().getIdentifier();
    }

    /**
     * Get the binary name for the current platform.
     *
     * @return Binary name ("clicker" or "clicker.exe" on Windows)
     */
    public static String getBinaryName() {
        return getOS() == OS.WINDOWS ? "clicker.exe" : "clicker";
    }

    /**
     * Get the cache directory for extracted binaries.
     *
     * <ul>
     *   <li>macOS: ~/Library/Caches/vibium</li>
     *   <li>Linux: ~/.cache/vibium (or XDG_CACHE_HOME/vibium)</li>
     *   <li>Windows: %LOCALAPPDATA%/vibium</li>
     * </ul>
     *
     * @return Path to the cache directory
     */
    public static Path getCacheDir() {
        String home = System.getProperty("user.home");
        switch (getOS()) {
            case DARWIN:
                return Paths.get(home, "Library", "Caches", "vibium");
            case WINDOWS:
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null && !localAppData.isEmpty()) {
                    return Paths.get(localAppData, "vibium");
                }
                return Paths.get(home, "AppData", "Local", "vibium");
            default: // LINUX
                String xdgCache = System.getenv("XDG_CACHE_HOME");
                if (xdgCache != null && !xdgCache.isEmpty()) {
                    return Paths.get(xdgCache, "vibium");
                }
                return Paths.get(home, ".cache", "vibium");
        }
    }
}
