package de.murmelmeister.essentials;

import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MurmelEssentials extends JavaPlugin {
    private MySQL mySQL;

    private BukkitTask rankTask;
    private final ConcurrentMap<Player, Ranks> ranks = new ConcurrentHashMap<>();

    @Override
    public void onDisable() {
        if (rankTask != null && !rankTask.isCancelled()) rankTask.cancel();
        ranks.clear();
        mySQL.disconnect();
    }

    @Override
    public void onEnable() {
        this.mySQL = new MySQL(getSLF4JLogger());
        mySQL.connect();
        ListenerManager.register(this);
        rankTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : getServer().getOnlinePlayers())
                ranks.computeIfAbsent(player, user -> new Ranks()).update(this, player);
        }, 0L, 20L);
    }

    public static MurmelEssentials getInstance() {
        return getPlugin(MurmelEssentials.class);
    }

    public Group getGroup() {
        return MurmelAPI.getGroup();
    }

    public User getUser() {
        return MurmelAPI.getUser();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }
}
