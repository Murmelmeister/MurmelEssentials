package de.murmelmeister.essentials.configs;

import de.murmelmeister.essentials.configs.settings.Config;
import de.murmelmeister.essentials.configs.settings.Maintenance;
import de.murmelmeister.essentials.configs.settings.Permission;
import de.murmelmeister.murmelapi.settings.SettingsService;

public final class ConfigProvider {
    public static final String CONFIG_ID_PATH = "murmel.settings";

    public static Config load(SettingsService settings) {
        String tagId = CONFIG_ID_PATH + ".config";
        Config loadConfig = settings.get(tagId, Config.class);

        if (loadConfig == null) {
            loadConfig = Config.defaults();
            settings.set(tagId, loadConfig);
        }

        return loadConfig;
    }

    public static Permission loadPermissions(SettingsService settings) {
        String tagId = CONFIG_ID_PATH + ".permissions";
        Permission loadPermissions = settings.get(tagId, Permission.class);

        if (loadPermissions == null) {
            loadPermissions = Permission.defaults();
            settings.set(tagId, loadPermissions);
        }

        return loadPermissions;
    }

    public static Maintenance loadMaintenance(SettingsService settings) {
        String tagId = CONFIG_ID_PATH + ".maintenance";
        Maintenance loadMaintenance = settings.get(tagId, Maintenance.class);

        if (loadMaintenance == null) {
            loadMaintenance = Maintenance.defaults();
            settings.set(tagId, loadMaintenance);
        }

        return loadMaintenance;
    }
}
