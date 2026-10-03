package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configurations.PluginConfig;
import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;

public class TranslatorListener {
    private final LanguageTypeProvider languageProvider;
    private final UserProvider userProvider;

    private final PluginConfig config;

    public TranslatorListener(MurmelEssentials plugin) {
        this.languageProvider = plugin.getLanguageProvider();
        this.userProvider = plugin.getUserProvider();
        this.config = plugin.getPluginConfig();
    }

    @Subscribe
    public void handlePlayerSettings(PlayerSettingsChangedEvent event) {
        if (!config.getBoolean(ConfigValue.LANGUAGE_CLIENT_FETCH)) return;

        PlayerSettings settings = event.getPlayerSettings();
        LanguageType language = languageProvider.findByCode(settings.getLocale().toLanguageTag()).orElse(null);
        if (language == null) return;

        User user = userProvider.findByMojangId(event.getPlayer().getUniqueId()).orElse(null);
        if (user == null) return;

        userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id());
    }
}
