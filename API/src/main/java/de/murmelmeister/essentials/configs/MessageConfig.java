package de.murmelmeister.essentials.configs;

import de.murmelmeister.murmelapi.language.message.Message;
import de.murmelmeister.murmelapi.language.message.MessageProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public final class MessageConfig {
    private final Path dataDirectory;

    public MessageConfig(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory must not be null")
                .toAbsolutePath()
                .normalize();
        createFile("lang/message_en.properties");
        createFile("lang/message_de.properties");
    }

    public MessageConfig(String pluginName) {
        this(pluginDirectory(pluginName));
    }

    // TODO: Version check in the files ( => maybe delete old messages or something)

    public void createFile(String file) {
        Path target = resolveFile(file);

        if (Files.notExists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (Exception e) {
                throw new RuntimeException("Could not create data directory for plugin.", e);
            }
        }

        if (Files.notExists(target)) {
            try {
                Files.createDirectories(target.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory for " + file + " file.", e);
            }

            String resourceName = file.replace('\\', '/');
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                if (in == null) throw new IllegalStateException("Could not find " + file + " file.");
                Files.copy(in, target);
            } catch (Exception e) {
                throw new RuntimeException("Could not create " + file + " file.", e);
            }
        }
    }

    public int[] loadToDatabase(MessageProvider provider, String file) {
        Objects.requireNonNull(provider, "provider must not be null");
        createFile(file);

        Properties properties = loadProperties(file);
        Collection<Message> messages = new ArrayList<>();

        String languageValue = properties.getProperty("language.id");
        if (languageValue == null || languageValue.isBlank())
            throw new IllegalArgumentException("Missing language.id in " + file + ".");

        int language;
        try {
            language = Integer.parseInt(languageValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid language.id in " + file + ": " + languageValue, exception);
        }
        if (language < 1)
            throw new IllegalArgumentException("language.id must be greater than zero in " + file + ".");

        for (String key : properties.stringPropertyNames()) {
            if ("language.id".equals(key)) continue;

            String value = properties.getProperty(key);
            if (key.isBlank() || value == null || value.isBlank())
                throw new IllegalArgumentException("Message " + key + " must not be blank in " + file + ".");

            boolean unchanged = provider.findMessage(key, language)
                    .map(existing -> existing.message().equals(value))
                    .orElse(false);
            if (!unchanged)
                messages.add(Message.of(0, key, language, value));
        }

        if (messages.isEmpty()) return new int[0];
        return provider.upsertAll(messages);
    }

    public int[] loadToDatabase(MessageProvider provider, List<String> files) {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(files, "files must not be null");
        if (files.isEmpty()) return new int[0];

        List<int[]> results = new ArrayList<>(files.size());
        int resultSize = 0;
        for (String file : files) {
            int[] fileResult = loadToDatabase(provider, file);
            results.add(fileResult);
            resultSize += fileResult.length;
        }

        int[] combinedResult = new int[resultSize];
        int offset = 0;
        for (int[] result : results) {
            System.arraycopy(result, 0, combinedResult, offset, result.length);
            offset += result.length;
        }
        return combinedResult;
    }

    private Properties loadProperties(String file) {
        Path path = resolveFile(file);
        try {
            return loadProperties(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException exception) {
            try {
                return loadProperties(path, StandardCharsets.ISO_8859_1);
            } catch (IOException fallbackException) {
                fallbackException.addSuppressed(exception);
                throw new RuntimeException("Could not load " + file + " file.", fallbackException);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load " + file + " file.", e);
        }
    }

    private Properties loadProperties(Path path, Charset charset) throws IOException {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            properties.load(reader);
        }
        return properties;
    }

    private Path resolveFile(String file) {
        if (file == null || file.isBlank())
            throw new IllegalArgumentException("file must not be blank");

        Path path = dataDirectory.resolve(file).toAbsolutePath().normalize();
        if (!path.startsWith(dataDirectory))
            throw new IllegalArgumentException("File must be inside the plugin data directory: " + file);
        return path;
    }

    private static Path pluginDirectory(String pluginName) {
        if (pluginName == null || pluginName.isBlank())
            throw new IllegalArgumentException("pluginName must not be blank");
        if (pluginName.contains("/") || pluginName.contains("\\") || ".".equals(pluginName) || "..".equals(pluginName))
            throw new IllegalArgumentException("pluginName must be a single directory name");
        return Path.of("plugins", pluginName);
    }
}
