package de.murmelmeister.essentials.files;

import de.murmelmeister.murmelapi.utils.Database;

public final class MySQL {
    public static void connect() {
        Database.connectEnv("DB_DRIVER", "DB_HOSTNAME", "DB_PORT", "DB_DATABASE", "DB_USERNAME", "DB_PASSWORD");
    }

    public static void disconnect() {
        Database.disconnect();
    }
}
