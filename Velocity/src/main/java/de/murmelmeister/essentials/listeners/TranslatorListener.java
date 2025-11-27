package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;

public class TranslatorListener {
    private final LanguageProvider languageProvider;
    private final UserProvider userProvider;

    public TranslatorListener(MurmelEssentials plugin) {
        this.languageProvider = plugin.getLanguageProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Subscribe
    public void handlePlayerSettings(PlayerSettingsChangedEvent event) {
        PlayerSettings settings = event.getPlayerSettings();
        Language language = languageProvider.get(settings.getLocale().toLanguageTag());
        if (language == null) return;

        User user = userProvider.findByMojangId(event.getPlayer().getUniqueId());
        if (user == null) return;

        userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id());
    }
}
