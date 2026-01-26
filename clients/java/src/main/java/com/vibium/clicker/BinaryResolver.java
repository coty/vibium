package com.vibium.clicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Resolves the path to the clicker binary.
 *
 * <p>Search order:
 * <ol>
 *   <li>Explicit path (if provided)</li>
 *   <li>VIBIUM_CLICKER_PATH or CLICKER_PATH environment variable</li>
 *   <li>Bundled binary in JAR resources (extracted to cache)</li>
 *   <li>System PATH</li>
 *   <li>Cache directory</li>
 *   <li>Local development paths</li>
 * </ol>
 */
public class BinaryResolver {

    private static final Logger log = LoggerFactory.getLogger(BinaryResolver.class);
    private static final String VERSION = "0.1.4";

    private BinaryResolver() {
        // Static utility class
    }

    /**
     * Resolve the clicker binary path.
     *
     * @return Path to the clicker binary
     * @throws IllegalStateException If the binary cannot be found
     */
    public static String resolve() {
        return resolve(null);
    }

    /**
     * Resolve the clicker binary path with an optional explicit path.
     *
     * @param explicitPath Optional explicit path (highest priority)
     * @return Path to the clicker binary
     * @throws IllegalStateException If the binary cannot be found
     */
    public static String resolve(String explicitPath) {
        String binaryName = Platform.getBinaryName();

        // 1. Explicit path
        if (explicitPath != null && !explicitPath.isEmpty()) {
            File file = new File(explicitPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using explicit binary path: {}", explicitPath);
                return explicitPath;
            }
            throw new IllegalStateException(
                    "Clicker binary not found at explicit path: " + explicitPath);
        }

        // 2. Environment variables
        String envPath = System.getenv("VIBIUM_CLICKER_PATH");
        if (envPath == null || envPath.isEmpty()) {
            envPath = System.getenv("CLICKER_PATH");
        }
        if (envPath != null && !envPath.isEmpty()) {
            File file = new File(envPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using binary from environment variable: {}", envPath);
                return envPath;
            }
        }

        // 3. Bundled binary (extract from JAR)
        String bundledPath = extractBundledBinary();
        if (bundledPath != null) {
            log.debug("Using bundled binary: {}", bundledPath);
            return bundledPath;
        }

        // 4. System PATH
        String pathBinary = findInPath(binaryName);
        if (pathBinary != null) {
            log.debug("Using binary from PATH: {}", pathBinary);
            return pathBinary;
        }

        // 5. Cache directory
        Path cacheBinary = Platform.getCacheDir().resolve(binaryName);
        if (Files.isExecutable(cacheBinary)) {
            log.debug("Using binary from cache: {}", cacheBinary);
            return cacheBinary.toString();
        }

        // 6. Local development paths
        String[] localPaths = {
                // From vibium/ root
                "clicker/bin/" + binaryName,
                // From clients/java/
                "../../clicker/bin/" + binaryName,
        };
        String cwd = System.getProperty("user.dir");
        for (String localPath : localPaths) {
            File localFile = new File(cwd, localPath);
            if (localFile.exists() && localFile.canExecute()) {
                log.debug("Using local development binary: {}", localFile.getAbsolutePath());
                return localFile.getAbsolutePath();
            }
        }

        throw new IllegalStateException(
                "Could not find clicker binary. Options:\n" +
                "  1. Set VIBIUM_CLICKER_PATH environment variable\n" +
                "  2. Add clicker to your PATH\n" +
                "  3. Build from source: make build-go");
    }

    /**
     * Extract bundled binary from JAR resources to cache directory.
     *
     * @return Path to extracted binary, or null if not bundled
     */
    private static String extractBundledBinary() {
        String platform = Platform.getPlatformIdentifier();
        String binaryName = Platform.getBinaryName();
        String resourcePath = "/" + platform + "/bin/" + binaryName;

        try (InputStream is = BinaryResolver.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.debug("No bundled binary found at {}", resourcePath);
                return null;
            }

            Path cacheDir = Platform.getCacheDir().resolve("clicker").resolve(VERSION);
            Files.createDirectories(cacheDir);
            Path targetPath = cacheDir.resolve(binaryName);

            // Return existing if already extracted
            if (Files.isExecutable(targetPath)) {
                return targetPath.toString();
            }

            // Extract to temp, then atomic move
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
                        // Not a POSIX filesystem, try chmod
                        tempFile.toFile().setExecutable(true);
                    }
                }

                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Extracted bundled binary to {}", targetPath);
                return targetPath.toString();
            } finally {
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
     * @param binaryName The binary name to search for
     * @return Path to binary if found, null otherwise
     */
    private static String findInPath(String binaryName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return null;
        }

        for (String dir : pathEnv.split(File.pathSeparator)) {
            File file = new File(dir, binaryName);
            if (file.exists() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
}
