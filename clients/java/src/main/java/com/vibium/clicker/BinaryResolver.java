package com.vibium.clicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the path to the clicker binary.
 *
 * Search order:
 * 1. Explicit path (if provided)
 * 2. CLICKER_PATH environment variable
 * 3. Local development paths (relative to cwd)
 */
public class BinaryResolver {

    private static final Logger log = LoggerFactory.getLogger(BinaryResolver.class);

    /**
     * Resolve the clicker binary path.
     *
     * @param explicitPath Optional explicit path (highest priority)
     * @return Path to the clicker binary
     * @throws IllegalStateException if binary cannot be found
     */
    public static String resolve(String explicitPath) {
        // 1. Explicit path
        if (explicitPath != null && !explicitPath.isEmpty()) {
            File file = new File(explicitPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using explicit clicker path: {}", explicitPath);
                return explicitPath;
            }
            throw new IllegalStateException("Clicker binary not found at explicit path: " + explicitPath);
        }

        // 2. Environment variable
        String envPath = System.getenv("CLICKER_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            File file = new File(envPath);
            if (file.exists() && file.canExecute()) {
                log.debug("Using CLICKER_PATH: {}", envPath);
                return envPath;
            }
            log.warn("CLICKER_PATH set but binary not found: {}", envPath);
        }

        // 3. Local development paths
        String binaryName = Platform.getBinaryName();
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
            "Could not find clicker binary. " +
            "Set CLICKER_PATH environment variable or ensure binary is built at clicker/bin/" + binaryName
        );
    }

    /**
     * Resolve the clicker binary path using default search.
     */
    public static String resolve() {
        return resolve(null);
    }
}
