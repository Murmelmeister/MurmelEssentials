package de.murmelmeister.essentials;

import de.murmelmeister.essentials.files.MySQL;
import org.bukkit.plugin.java.JavaPlugin;

public final class MurmelEssentials extends JavaPlugin {
    private final MySQL mySQL;

    @Override
    public void onDisable() {
        mySQL.disconnect();
    }

    @Override
    public void onEnable() {
        mySQL.connect();
    }

    public MurmelEssentials() {
        this.mySQL = new MySQL(getSLF4JLogger());
    }

    public MurmelEssentials getInstance() {
        return this;
    }
}
