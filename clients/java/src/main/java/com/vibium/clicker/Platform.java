package com.vibium.clicker;

/**
 * Platform and architecture detection.
 */
public class Platform {

    public enum OS {
        LINUX("linux"),
        DARWIN("darwin"),
        WINDOWS("win32");

        private final String identifier;

        OS(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    public enum Arch {
        X64("x64"),
        ARM64("arm64");

        private final String identifier;

        Arch(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * Get the current operating system.
     */
    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return OS.LINUX;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return OS.DARWIN;
        } else if (os.contains("windows")) {
            return OS.WINDOWS;
        }
        throw new UnsupportedOperationException("Unsupported operating system: " + os);
    }

    /**
     * Get the current architecture.
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
     * Get the platform identifier (e.g., "darwin-arm64").
     */
    public static String getPlatformIdentifier() {
        return getOS().getIdentifier() + "-" + getArch().getIdentifier();
    }

    /**
     * Get the binary name for the current platform.
     */
    public static String getBinaryName() {
        return getOS() == OS.WINDOWS ? "clicker.exe" : "clicker";
    }
}
