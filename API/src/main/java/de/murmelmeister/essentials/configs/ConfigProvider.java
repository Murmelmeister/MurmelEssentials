package de.murmelmeister.essentials.configs;

import de.murmelmeister.essentials.configs.settings.Maintenance;
import de.murmelmeister.murmelapi.settings.SettingsService;

public final class ConfigProvider {
    public static final String CONFIG_ID_PATH = "murmel.settings";

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
