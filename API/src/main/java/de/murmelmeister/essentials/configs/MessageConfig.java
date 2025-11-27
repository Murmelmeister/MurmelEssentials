package de.murmelmeister.essentials.configs;

import de.murmelmeister.murmelapi.language.message.MessageProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MessageConfig {
    private final Path dataDirectory;

    public MessageConfig(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        createFile("lang/message_en.properties");
        createFile("lang/message_de.properties");
    }

    public MessageConfig(String pluginName) {
        this.dataDirectory = Path.of("./plugins/" + pluginName + "/");
        createFile("lang/message_en.properties");
        createFile("lang/message_de.properties");
    }

    // TODO: Version check in the files ( => maybe delete old messages or something)

    public void createFile(String file) {
        if (Files.notExists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (Exception e) {
                throw new RuntimeException("Could not create data directory for plugin.", e);
            }
        }

        if (Files.notExists(dataDirectory.resolve(file))) {
            try {
                Files.createDirectories(dataDirectory.resolve(file).getParent());
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory for " + file + " file.", e);
            }

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
                if (in == null) throw new IllegalStateException("Could not find " + file + " file.");
                Files.copy(in, dataDirectory.resolve(file));
            } catch (Exception e) {
                throw new RuntimeException("Could not create " + file + " file.", e);
            }
        }
    }

    public int[] loadToDatabase(MessageProvider provider, String file) {
        Properties properties = loadProperties(file);
        return provider.upsertAll(properties);
    }

    public int[] loadToDatabase(MessageProvider provider, List<String> files) {
        if (files == null || files.isEmpty()) return new int[0];

        List<Properties> all = new ArrayList<>();
        for (String file : files)
            all.add(loadProperties(file));
        return provider.upsertAll(all);
    }

    private Properties loadProperties(String file) {
        Properties properties = new Properties();

        Path path = dataDirectory.resolve(file);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException("Could not load " + file + " file.", e);
        }

        return properties;
    }
}
