package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;

public class TranslatorListener {
    private final LanguageTypeProvider languageProvider;
    private final UserProvider userProvider;

    public TranslatorListener(MurmelEssentials plugin) {
        this.languageProvider = plugin.getLanguageProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Subscribe
    public void handlePlayerSettings(PlayerSettingsChangedEvent event) {
        PlayerSettings settings = event.getPlayerSettings();
        LanguageType language = languageProvider.findByCode(settings.getLocale().toLanguageTag()).orElse(null);
        if (language == null) return;

        User user = userProvider.findByMojangId(event.getPlayer().getUniqueId()).orElse(null);
        if (user == null) return;

        userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id());
    }
}
