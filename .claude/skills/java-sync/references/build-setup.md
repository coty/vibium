# Build Setup Reference

The Java client uses **Maven** for build management. This document covers required dependencies, binary bundling, and common build issues.

## Complete pom.xml Template

Use this as the canonical pom.xml for the Java client. Update the `<version>` to match the project VERSION file.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.vibium</groupId>
    <artifactId>vibium</artifactId>
    <version>0.1.4-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Vibium Java Client</name>
    <description>Java client library for Vibium browser automation</description>
    <url>https://github.com/anthropics/vibium</url>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependency versions -->
        <java-websocket.version>1.6.0</java-websocket.version>
        <gson.version>2.11.0</gson.version>
        <slf4j.version>2.0.16</slf4j.version>
        <junit.version>5.11.4</junit.version>
    </properties>

    <dependencies>
        <!-- WebSocket client -->
        <dependency>
            <groupId>org.java-websocket</groupId>
            <artifactId>Java-WebSocket</artifactId>
            <version>${java-websocket.version}</version>
        </dependency>

        <!-- JSON parsing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <!-- Include platform-specific clicker binaries as resources -->
        <resources>
            <resource>
                <directory>src/main/resources</directory>
            </resource>
            <!-- Platform binaries from shared packages/ directory (populated by make package-platforms) -->
            <resource>
                <directory>${project.basedir}/../../packages</directory>
                <includes>
                    <include>darwin-arm64/bin/clicker</include>
                    <include>darwin-x64/bin/clicker</include>
                    <include>linux-x64/bin/clicker</include>
                    <include>linux-arm64/bin/clicker</include>
                    <include>win32-x64/bin/clicker.exe</include>
                </includes>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.2</version>
            </plugin>
            <!-- Generate fat JAR with all dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.vibium.Browser</mainClass>
                                </transformer>
                                <!-- Merge SLF4J service files -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <shadedArtifactAttached>true</shadedArtifactAttached>
                            <shadedClassifierName>all</shadedClassifierName>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Generate sources JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Required Dependencies

### Runtime Dependencies

```xml
<!-- WebSocket client for BiDi protocol -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.6.0</version>
</dependency>

<!-- JSON parsing for BiDi messages -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>

<!-- Logging API -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
</dependency>
```

### Test Dependencies

```xml
<!-- JUnit 5 for testing -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.4</version>
    <scope>test</scope>
</dependency>

<!-- SLF4J simple implementation for test logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.16</version>
    <scope>test</scope>
</dependency>
```

## Java Version

**Minimum: Java 17**

Required for:
- Records (immutable data types)
- Text blocks (embedded JavaScript)
- Pattern matching enhancements
- Sealed classes (if used for protocol types)

Set in pom.xml:
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

## Binary Bundling

The Java client must bundle platform-specific clicker binaries as resources.

### Directory Structure

Binaries are shared across all clients via the `packages/` directory at repo root:

```
packages/
  darwin-arm64/bin/clicker
  darwin-x64/bin/clicker
  linux-x64/bin/clicker
  linux-arm64/bin/clicker
  win32-x64/bin/clicker.exe
```

### Maven Resource Configuration

Include binaries in the JAR:

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
        </resource>
        <!-- Platform binaries from shared packages/ directory -->
        <resource>
            <directory>${project.basedir}/../../packages</directory>
            <includes>
                <include>darwin-arm64/bin/clicker</include>
                <include>darwin-x64/bin/clicker</include>
                <include>linux-x64/bin/clicker</include>
                <include>linux-arm64/bin/clicker</include>
                <include>win32-x64/bin/clicker.exe</include>
            </includes>
        </resource>
    </resources>
</build>
```

**Important:** Binaries must be built first using `make package-platforms` from repo root before Maven can bundle them.

### Runtime Binary Resolution

The Java client extracts the appropriate binary at runtime based on OS/architecture detection. See `clients/java/src/main/java/com/vibium/clicker/BinaryResolver.java` for the extraction logic.

## Maven Plugins

### Compiler Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
    </configuration>
</plugin>
```

### Surefire (Test Runner)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
</plugin>
```

### Shade Plugin (Fat JAR)

Creates an uber-JAR with all dependencies bundled:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>com.vibium.Browser</mainClass>
                    </transformer>
                    <!-- Merge SLF4J service files -->
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                </transformers>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
                <createDependencyReducedPom>false</createDependencyReducedPom>
                <shadedArtifactAttached>true</shadedArtifactAttached>
                <shadedClassifierName>all</shadedClassifierName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Source Plugin

Generates sources JAR for distribution:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-source-plugin</artifactId>
    <version>3.3.1</version>
    <executions>
        <execution>
            <id>attach-sources</id>
            <goals>
                <goal>jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Build Commands

From `clients/java/` directory:

```bash
# Compile source code
mvn compile

# Run tests
mvn test

# Package JAR (includes fat JAR with all dependencies)
mvn package

# Clean build artifacts
mvn clean

# Full clean build with tests
mvn clean package

# Install to local Maven repository
mvn install

# Skip tests during packaging
mvn package -DskipTests
```

## Common Issues

### 1. Missing Binaries

**Error:** Binary not found in JAR resources

**Solution:** Run `make package-platforms` from repo root before `mvn package`

### 2. WebSocket Version Conflicts

**Error:** ClassNotFoundException or NoSuchMethodError with WebSocket classes

**Solution:** Ensure `java-websocket` version is 1.6.0 or compatible. Check for transitive dependency conflicts with `mvn dependency:tree`

### 3. Gson Serialization Issues

**Error:** JSON parsing failures for BiDi protocol messages

**Solution:** Use gson 2.11.0+ for proper handling of modern JSON features. Consider custom type adapters for complex protocol types.

### 4. SLF4J Binding Warnings

**Warning:** "SLF4J: Failed to load class org.slf4j.impl.StaticLoggerBinder"

**Solution:** Include an SLF4J implementation (slf4j-simple, logback, etc.) in runtime scope. Test scope is sufficient for development.

### 5. Java Version Mismatch

**Error:** Unsupported class file major version or language features not recognized

**Solution:** Verify Java 17+ is installed and JAVA_HOME points to correct version. Check IDE/Maven settings match pom.xml compiler configuration.

## Version Alignment

Keep Java client version aligned with the project:

1. **Source of truth:** The root `VERSION` file contains the canonical version
2. **Update two files:**
   - `clients/java/pom.xml` - Set `<version>X.Y.Z-SNAPSHOT</version>`
   - `clients/java/src/main/java/com/vibium/clicker/BinaryResolver.java` - Set `VERSION = "X.Y.Z"`
3. Use `-SNAPSHOT` suffix in pom.xml during development
4. Remove `-SNAPSHOT` for release builds
5. The BinaryResolver VERSION controls the cache path for extracted binaries (`~/.cache/vibium/clicker/<VERSION>/`)
