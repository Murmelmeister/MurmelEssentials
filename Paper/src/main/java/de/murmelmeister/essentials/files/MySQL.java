package de.murmelmeister.essentials.files;

import de.murmelmeister.murmelapi.utils.Database;
import de.murmelmeister.murmelapi.utils.FileUtil;
import org.slf4j.Logger;

import java.io.File;
import java.util.Properties;

public final class MySQL {
    private final Logger logger;
    private final File file;

    public MySQL(Logger logger) {
        this.logger = logger;
        this.file = FileUtil.createFile(logger, "./MurmelProperties", "mysql.properties");
    }

    public void connect() {
        Properties properties = FileUtil.loadProperties(logger, file);
        Database.connect(properties.getProperty("DB_DRIVER"), properties.getProperty("DB_HOSTNAME"), properties.getProperty("DB_PORT"), properties.getProperty("DB_DATABASE"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
        logger.info("Connected to the database.");
    }

    public void disconnect() {
        Database.disconnect();
        logger.info("Disconnected from the database.");
    }
}
