package de.murmelmeister.essentials.files;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.utils.FileUtil;
import org.slf4j.Logger;

import java.io.File;
import java.util.Properties;

public final class MySQL {
    private final File file;

    public MySQL(Logger logger) {
        this.file = FileUtil.createFile(logger, "./", "mysql.properties");
    }

    public void connect() {
        Properties properties = FileUtil.loadProperties(file);
        String databaseName = properties.getProperty("DB_DATABASE");
        String dbDriver = properties.getProperty("DB_DRIVER");
        String dbHostname = properties.getProperty("DB_HOSTNAME");
        String dbPort = properties.getProperty("DB_PORT");
        String dbUsername = properties.getProperty("DB_USERNAME");
        String dbPassword = properties.getProperty("DB_PASSWORD");
        if (databaseName == null || dbDriver == null || dbHostname == null || dbPort == null || dbUsername == null || dbPassword == null)
            throw new IllegalArgumentException("Database properties are not set correctly.");

        MurmelAPI.setDatabaseName(databaseName);
        String url = String.format("jdbc:%s://%s:%s/%s", dbDriver, dbHostname, dbPort, databaseName);
        MurmelAPI.connect(url, dbUsername, dbPassword);
    }

    public void disconnect() {
        MurmelAPI.disconnect();
    }
}
