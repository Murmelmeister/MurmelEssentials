package de.murmelmeister.essentials.configs;

import de.murmelmeister.murmelapi.MurmelAPI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String CONFIG_FILE = "database.properties";
    private static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";

    private final Path dataDirectory;
    private final Path configPath;
    private final MurmelAPI murmelAPI;
    private boolean connected;

    public DatabaseConfig(Path dataDirectory, MurmelAPI murmelAPI) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory must not be null")
                .toAbsolutePath()
                .normalize();
        this.configPath = this.dataDirectory.resolve(CONFIG_FILE);
        this.murmelAPI = Objects.requireNonNull(murmelAPI, "murmelAPI must not be null");
        createFile();
    }

    public DatabaseConfig(String pluginName, MurmelAPI murmelAPI) {
        this(pluginDirectory(pluginName), murmelAPI);
    }

    public void createFile() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Could not create plugin data directory: " + dataDirectory, exception);
        }

        if (Files.exists(configPath)) return;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null)
                throw new IllegalStateException("Could not find bundled " + CONFIG_FILE + ".");
            Files.copy(input, configPath);
        } catch (IOException exception) {
            throw new RuntimeException("Could not create " + configPath + ".", exception);
        }
    }

    public synchronized void connect() {
        if (connected) return;

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new RuntimeException("Could not load " + configPath + ".", exception);
        }

        String url = properties.getProperty("jdbcUrl");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("Missing jdbcUrl in " + configPath + ".");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Missing username in " + configPath + ".");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Missing password in " + configPath + ".");

        properties.setProperty("jdbcUrl", url.trim());
        properties.setProperty("username", username.trim());
        properties.putIfAbsent("driverClassName", MARIADB_DRIVER);

        murmelAPI.connect(properties);
        connected = true;
    }

    public synchronized void disconnect() {
        if (!connected) return;
        murmelAPI.disconnect();
        connected = false;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    private static Path pluginDirectory(String pluginName) {
        if (pluginName == null || pluginName.isBlank())
            throw new IllegalArgumentException("pluginName must not be blank");
        if (pluginName.contains("/") || pluginName.contains("\\") || ".".equals(pluginName) || "..".equals(pluginName))
            throw new IllegalArgumentException("pluginName must be a single directory name");
        return Path.of("plugins", pluginName);
    }
}
