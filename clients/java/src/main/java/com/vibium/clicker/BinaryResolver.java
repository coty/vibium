package com.vibium.clicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Resolves the path to the clicker binary.
 *
 * Search order:
 * 1. Explicit path (if provided)
 * 2. VIBIUM_CLICKER_PATH or CLICKER_PATH environment variable
 * 3. Bundled binary in JAR resources (extracted to cache)
 * 4. System PATH
 * 5. Cache directory
 * 6. Local development paths (relative to cwd)
 */
public class BinaryResolver {

    private static final Logger log = LoggerFactory.getLogger(BinaryResolver.class);
    private static final String VERSION = "0.1.2";

    /**
     * Resolve the clicker binary path.
     *
     * @param explicitPath Optional explicit path (highest priority)
     * @return Path to the clicker binary
     * @throws IllegalStateException if binary cannot be found
     */
    public static String resolve(String explicitPath) {
        String binaryName = Platform.getBinaryName();

        // 1. Explicit path
        if (explicitPath != null && !explicitPath.isEmpty()) {
            File file = new File(explicitPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using explicit clicker path: {}", explicitPath);
                return explicitPath;
            }
            throw new IllegalStateException("Clicker binary not found at explicit path: " + explicitPath);
        }

        // 2. Environment variable (VIBIUM_CLICKER_PATH or CLICKER_PATH)
        String envPath = System.getenv("VIBIUM_CLICKER_PATH");
        if (envPath == null || envPath.isEmpty()) {
            envPath = System.getenv("CLICKER_PATH");
        }
        if (envPath != null && !envPath.isEmpty()) {
            File file = new File(envPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using env var clicker path: {}", envPath);
                return envPath;
            }
            log.warn("Environment variable set but binary not found: {}", envPath);
        }

        // 3. Bundled binary in JAR resources (extract to cache)
        String bundledPath = extractBundledBinary();
        if (bundledPath != null) {
            log.debug("Using bundled clicker from JAR: {}", bundledPath);
            return bundledPath;
        }

        // 4. System PATH
        String pathBinary = findInPath(binaryName);
        if (pathBinary != null) {
            log.debug("Using clicker from PATH: {}", pathBinary);
            return pathBinary;
        }

        // 5. Cache directory (may have been placed there manually or by another tool)
        Path cacheDir = Platform.getCacheDir();
        Path cacheBinary = cacheDir.resolve(binaryName);
        if (Files.isExecutable(cacheBinary)) {
            log.debug("Using clicker from cache: {}", cacheBinary);
            return cacheBinary.toString();
        }

        // 6. Local development paths
        String cwd = System.getProperty("user.dir");
        String[] localPaths = {
            // From vibium/ root
            Paths.get(cwd, "clicker", "bin", binaryName).toString(),
            // From clients/java/
            Paths.get(cwd, "..", "..", "clicker", "bin", binaryName).toString(),
            // From tests/java/
            Paths.get(cwd, "..", "..", "..", "clicker", "bin", binaryName).toString(),
        };

        for (String localPath : localPaths) {
            File file = new File(localPath);
            if (file.exists() && file.canExecute()) {
                String absolutePath = file.getAbsolutePath();
                log.debug("Using local clicker path: {}", absolutePath);
                return absolutePath;
            }
        }

        throw new IllegalStateException(
            "Could not find clicker binary. Options:\n" +
            "  1. Set VIBIUM_CLICKER_PATH environment variable\n" +
            "  2. Add clicker to your PATH\n" +
            "  3. Build from source: make build-go"
        );
    }

    /**
     * Resolve the clicker binary path using default search.
     */
    public static String resolve() {
        return resolve(null);
    }

    /**
     * Extract bundled binary from JAR resources to cache directory.
     *
     * @return Path to extracted binary, or null if not bundled
     */
    private static String extractBundledBinary() {
        String platform = Platform.getPlatformIdentifier();
        String binaryName = Platform.getBinaryName();
        // Resource path matches npm package structure: /{platform}/bin/clicker
        String resourcePath = "/" + platform + "/bin/" + binaryName;

        try (InputStream is = BinaryResolver.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.debug("No bundled binary found at resource path: {}", resourcePath);
                return null;
            }

            // Extract to cache: ~/.cache/vibium/clicker/{version}/clicker
            Path cacheDir = Platform.getCacheDir().resolve("clicker").resolve(VERSION);
            Files.createDirectories(cacheDir);

            Path targetPath = cacheDir.resolve(binaryName);

            // Check if already extracted
            if (Files.isExecutable(targetPath)) {
                return targetPath.toString();
            }

            // Extract to temp file first, then move atomically
            Path tempFile = cacheDir.resolve(binaryName + ".tmp." + System.currentTimeMillis());
            try {
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                // Make executable on Unix
                if (Platform.getOS() != Platform.OS.WINDOWS) {
                    try {
                        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tempFile);
                        perms.add(PosixFilePermission.OWNER_EXECUTE);
                        perms.add(PosixFilePermission.GROUP_EXECUTE);
                        perms.add(PosixFilePermission.OTHERS_EXECUTE);
                        Files.setPosixFilePermissions(tempFile, perms);
                    } catch (UnsupportedOperationException e) {
                        // Non-POSIX filesystem, skip
                    }
                }

                // Atomic move
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Extracted clicker binary to: {}", targetPath);
                return targetPath.toString();

            } finally {
                // Clean up temp file if move failed
                Files.deleteIfExists(tempFile);
            }

        } catch (IOException e) {
            log.warn("Failed to extract bundled binary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Find binary in system PATH.
     *
     * @param binaryName Name of the binary to find
     * @return Full path to binary, or null if not found
     */
    private static String findInPath(String binaryName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }

        String pathSeparator = File.pathSeparator;
        for (String dir : pathEnv.split(pathSeparator)) {
            File file = new File(dir, binaryName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}
