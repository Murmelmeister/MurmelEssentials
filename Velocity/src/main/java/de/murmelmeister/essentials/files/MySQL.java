package de.murmelmeister.essentials.files;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;
import de.murmelmeister.murmelapi.utils.FileUtil;
import org.slf4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.Properties;

public final class MySQL {
    private final File file;
    private Group group;
    private User user;
    private Permission permission;

    public MySQL(Logger logger) {
        this.file = FileUtil.createFile(logger, "./MurmelProperties", "mysql.properties");
    }

    public void connect() {
        Properties properties = FileUtil.loadProperties(file);
        Database.connect(properties.getProperty("DB_DRIVER"), properties.getProperty("DB_HOSTNAME"), properties.getProperty("DB_PORT"), properties.getProperty("DB_DATABASE"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));
    }

    public void disconnect() {
        Database.disconnect();
    }

    public void load() throws SQLException {
        this.group = MurmelAPI.getGroup();
        this.user = MurmelAPI.getUser();
        this.permission = MurmelAPI.getPermission(group, user);
    }

    public Group getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public Permission getPermission() {
        return permission;
    }
}
