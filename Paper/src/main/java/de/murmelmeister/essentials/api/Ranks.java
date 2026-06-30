package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configs.ConfigProvider;
import de.murmelmeister.essentials.configs.settings.Permission;
import de.murmelmeister.essentials.utils.ChatFormatter;
import de.murmelmeister.library.utils.AnimationUtils;
import de.murmelmeister.murmelapi.clan.Clan;
import de.murmelmeister.murmelapi.clan.ClanProvider;
import de.murmelmeister.murmelapi.clan.member.ClanMember;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProvider;
import de.murmelmeister.murmelapi.color.PrefixColor;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.color.UserPrefixColor;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Ranks implements MurmelCache {
    private static final long DISPLAY_UPDATE_INTERVAL_TICKS = 2L;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicBoolean hasUpdated = new AtomicBoolean(false);
    private BukkitTask task;

    private final RefreshProvider refreshProvider;
    private final GroupProvider groupProvider;
    private final GroupColorProvider groupColorProvider;
    private final UserProvider userProvider;
    private final SettingsService settingsService;

    private final PermissionService permissionService;

    private final PrefixColorProvider colorProvider;
    private final UserPrefixColorProvider userColorProvider;
    private final ClanProvider clanProvider;
    private final ClanMemberProvider clanMemberProvider;

    public Ranks(@NotNull MurmelEssentials plugin) {
        this.refreshProvider = plugin.getRefreshProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.groupColorProvider = plugin.getGroupColorProvider();
        this.userProvider = plugin.getUserProvider();
        this.settingsService = plugin.getSettingsService();
        this.permissionService = plugin.getPermissionService();
        this.colorProvider = plugin.getPrefixColorProvider();
        this.userColorProvider = plugin.getUserPrefixColorProvider();
        this.clanProvider = plugin.getClanProvider();
        this.clanMemberProvider = plugin.getClanMemberProvider();
        this.refreshProvider.register(this);
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled())
            task.cancel();
    }

    public void updatePlayers(@NotNull MurmelEssentials plugin, @NotNull Server server) {
        // Recommended to use Scoreboard/Player-API in the main thread
        task = server.getScheduler().runTaskTimer(plugin, () -> {
            server.getOnlinePlayers().forEach(player -> {
                User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
                if (user == null) return;
                int userId = user.id();

                UserPrefixColor userColor = userColorProvider.findActiveById(userId).orElse(null);
                if (userColor == null) return;

                PrefixColor prefixColor = colorProvider.findById(userColor.colorId()).orElse(null);
                if (prefixColor == null) return;
                if (!prefixColor.animated()) return;
                setPlayerListName(player);
            });
            if (hasUpdated.get()) {
                server.getOnlinePlayers().forEach(player -> {
                    setPlayerTeams(player);
                    setPlayerListName(player);
                    player.updateCommands();
                });
                hasUpdated.set(false);
            }
        }, DISPLAY_UPDATE_INTERVAL_TICKS, DISPLAY_UPDATE_INTERVAL_TICKS);
    }

    public void setChatFormat(@NotNull AsyncChatEvent event) {
        // Get the user
        Player player = event.getPlayer();
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;
        int userId = user.id();

        // Get the highest priority group for the user
        Group group = permissionService.getHighestGroup(PermissionTarget.user(userId));
        if (group == null) return;
        int groupId = group.id();

        // Load the config
        Permission permission = ConfigProvider.loadPermissions(settingsService);

        // Get the group colors
        GroupColor prefix = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_PREFIX.getId()).orElse(null);
        GroupColor suffix = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_SUFFIX.getId()).orElse(null);
        GroupColor color = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_COLOR.getId()).orElse(null);
        GroupColor chatMessage = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_MESSAGE.getId()).orElse(null);

        // Format the chat message
        String formattedPrefix = prefix != null ? prefix.value() : permission.defaultChatPrefix();
        String formattedSuffix = suffix != null ? suffix.value() : permission.defaultChatSuffix();
        String formattedColor = color != null ? color.value() : permission.defaultChatColor();
        String formattedColorMessage = chatMessage != null ? chatMessage.value() : permission.defaultChatColorMessage();

        // Get Clan
        ClanMember member = clanMemberProvider.findClan(userId)
                .stream()
                .findFirst()
                .orElse(null);
        Optional<Clan> clan = member != null ? clanProvider.findById(member.clanId()) : Optional.empty();
        TagResolver.Single clanSign = Placeholder.parsed("clan_sign", clan.map(c -> c.sign() != null ? c.sign() : "").orElse(""));
        TagResolver.Single clanTag = Placeholder.parsed("clan_tag", clan.map(c -> c.tag() != null ? c.tag() : "").orElse(""));

        Optional<UserPrefixColor> activeColor = userColorProvider.findActiveById(userId);
        Component baseComponent;

        if (activeColor.isPresent()) {
            Optional<PrefixColor> optPrefixColor = colorProvider.findById(activeColor.get().colorId());

            if (optPrefixColor.isPresent()) {
                PrefixColor prefixColor = optPrefixColor.get();

                if (prefixColor.animated()) {
                    // If the prefix color is animated, split the color string into multiple colors and animate them
                    List<String> colors = List.of(prefixColor.color().split(","));

                    if (colors.isEmpty()) {
                        // If the color string is empty, use the default formatting
                        baseComponent = miniMessage.deserialize(permission.chatFormat(),
                                Placeholder.parsed("color", formattedColor),
                                clanSign,
                                Placeholder.parsed("prefix", formattedPrefix),
                                Placeholder.parsed("username", player.getName()),
                                Placeholder.parsed("suffix", formattedSuffix),
                                clanTag
                        );
                    } else {
                        // If the color string is not empty, animate the prefix color
                        Component chatFormat = miniMessage.deserialize(permission.chatFormat(),
                                Placeholder.parsed("color", ""),
                                clanSign,
                                Placeholder.parsed("prefix", formattedPrefix),
                                Placeholder.parsed("username", player.getName()),
                                Placeholder.parsed("suffix", formattedSuffix),
                                clanTag
                        );

                        String animatedInput = PlainTextComponentSerializer.plainText().serialize(chatFormat);
                        Component animatedComponent = miniMessage.deserialize(
                                AnimationUtils.animatePerColorCycle(colors, animatedInput, 6.00f)
                        );
                        Map<Integer, TextColor> explicitColors = collectExplicitColorsByIndex(chatFormat);
                        baseComponent = applyExplicitColorsByIndex(animatedComponent, explicitColors);
                    }
                } else {
                    // If the prefix color is not animated, use it directly
                    baseComponent = miniMessage.deserialize(permission.chatFormat(),
                            Placeholder.parsed("color", prefixColor.color()),
                            clanSign,
                            Placeholder.parsed("prefix", formattedPrefix),
                            Placeholder.parsed("username", player.getName()),
                            Placeholder.parsed("suffix", formattedSuffix),
                            clanTag
                    );
                }
            } else {
                // If the prefix color is not found, use the default formatting
                baseComponent = miniMessage.deserialize(permission.chatFormat(),
                        Placeholder.parsed("color", formattedColor),
                        clanSign,
                        Placeholder.parsed("prefix", formattedPrefix),
                        Placeholder.parsed("username", player.getName()),
                        Placeholder.parsed("suffix", formattedSuffix),
                        clanTag
                );
            }
        } else {
            // If the user color is not found, use the default formatting
            baseComponent = miniMessage.deserialize(permission.chatFormat(),
                    Placeholder.parsed("color", formattedColor),
                    clanSign,
                    Placeholder.parsed("prefix", formattedPrefix),
                    Placeholder.parsed("username", player.getName()),
                    Placeholder.parsed("suffix", formattedSuffix),
                    clanTag
            );
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String finalMessage = PlainTextComponentSerializer.plainText().serialize(message);
            Component messageComponent = ChatFormatter.format(player, finalMessage, formattedColorMessage);
            return baseComponent.append(messageComponent);
        });
    }

    private void setPlayerListName(@NotNull Player player) {
        // Get the user
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;
        int userId = user.id();

        // Get the highest priority group for the user
        Group group = permissionService.getHighestGroup(PermissionTarget.user(userId));
        if (group == null) return;
        int groupId = group.id();

        // Load the config
        Permission permission = ConfigProvider.loadPermissions(settingsService);

        // Get the group colors
        GroupColor prefix = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_PREFIX.getId()).orElse(null);
        GroupColor suffix = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_SUFFIX.getId()).orElse(null);
        GroupColor color = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_COLOR.getId()).orElse(null);

        // Format the player list name
        String formattedPrefix = prefix != null ? prefix.value() : permission.defaultTabPrefix();
        String formattedSuffix = suffix != null ? suffix.value() : permission.defaultTabSuffix();
        String formattedColor = color != null ? color.value() : permission.defaultTabColor();

        // Get Clan
        ClanMember member = clanMemberProvider.findClan(userId)
                .stream()
                .findFirst()
                .orElse(null);
        Optional<Clan> clan = member != null ? clanProvider.findById(member.clanId()) : Optional.empty();
        TagResolver.Single clanSign = Placeholder.parsed("clan_sign", clan.map(c -> c.sign() != null ? c.sign() : "").orElse(""));
        TagResolver.Single clanTag = Placeholder.parsed("clan_tag", clan.map(c -> c.tag() != null ? c.tag() : "").orElse(""));

        Optional<UserPrefixColor> activeColor = userColorProvider.findActiveById(userId);
        Component baseComponent;

        if (activeColor.isPresent()) {
            Optional<PrefixColor> optPrefixColor = colorProvider.findById(activeColor.get().colorId());

            if (optPrefixColor.isPresent()) {
                PrefixColor prefixColor = optPrefixColor.get();

                if (prefixColor.animated()) {
                    // If the prefix color is animated, split the color string into multiple colors and animate them
                    List<String> colors = List.of(prefixColor.color().split(","));

                    if (colors.isEmpty()) {
                        // If the color string is empty, use the default formatting
                        baseComponent = miniMessage.deserialize(permission.tabFormat(),
                                Placeholder.parsed("color", formattedColor),
                                clanSign,
                                Placeholder.parsed("prefix", formattedPrefix),
                                Placeholder.parsed("username", player.getName()),
                                Placeholder.parsed("suffix", formattedSuffix),
                                clanTag
                        );
                    } else {
                        // If the color string is not empty, animate the prefix color
                        Component tabFormat = miniMessage.deserialize(permission.tabFormat(),
                                Placeholder.parsed("color", ""),
                                clanSign,
                                Placeholder.parsed("prefix", formattedPrefix),
                                Placeholder.parsed("username", player.getName()),
                                Placeholder.parsed("suffix", formattedSuffix),
                                clanTag
                        );

                        String animatedInput = PlainTextComponentSerializer.plainText().serialize(tabFormat);
                        Component animatedComponent = miniMessage.deserialize(
                                AnimationUtils.animatePerColorCycle(colors, animatedInput, 6.00f)
                        );
                        Map<Integer, TextColor> explicitColors = collectExplicitColorsByIndex(tabFormat);
                        baseComponent = applyExplicitColorsByIndex(animatedComponent, explicitColors);
                    }
                } else {
                    // If the prefix color is not animated, use it directly
                    baseComponent = miniMessage.deserialize(permission.tabFormat(),
                            Placeholder.parsed("color", prefixColor.color()),
                            clanSign,
                            Placeholder.parsed("prefix", formattedPrefix),
                            Placeholder.parsed("username", player.getName()),
                            Placeholder.parsed("suffix", formattedSuffix),
                            clanTag
                    );
                }
            } else {
                // If the prefix color is not found, use the default formatting
                baseComponent = miniMessage.deserialize(permission.tabFormat(),
                        Placeholder.parsed("color", formattedColor),
                        clanSign,
                        Placeholder.parsed("prefix", formattedPrefix),
                        Placeholder.parsed("username", player.getName()),
                        Placeholder.parsed("suffix", formattedSuffix),
                        clanTag
                );
            }
        } else {
            // If the user color is not found, use the default formatting
            baseComponent = miniMessage.deserialize(permission.tabFormat(),
                    Placeholder.parsed("color", formattedColor),
                    clanSign,
                    Placeholder.parsed("prefix", formattedPrefix),
                    Placeholder.parsed("username", player.getName()),
                    Placeholder.parsed("suffix", formattedSuffix),
                    clanTag
            );
        }

        player.setPlayerListOrder(group.priority());
        player.playerListName(baseComponent);
    }

    private void setPlayerTeams(@NotNull Player player) {
        Permission permission = ConfigProvider.loadPermissions(settingsService);

        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;
        Scoreboard scoreboard = player.getScoreboard();

        // Get the existing teams
        Map<String, Team> existingTeams = new HashMap<>();
        scoreboard.getTeams().forEach(team -> existingTeams.put(team.getName(), team));

        // Sort the players by priority
        Map<Integer, List<String>> playersBySortId = new HashMap<>();
        player.getServer().getOnlinePlayers().forEach(target -> {
            User targetUser = userProvider.findByMojangId(target.getUniqueId()).orElse(null);
            if (targetUser == null) return;

            Group targetGroup = permissionService.getHighestGroup(PermissionTarget.user(targetUser.id()));
            if (targetGroup == null) return;
            playersBySortId.computeIfAbsent(targetGroup.priority(), k -> new ArrayList<>()).add(target.getName());
        });

        // Get the team IDs by priority
        Map<Integer, String> teamIdByPriority = new HashMap<>();
        Set<String> validTeamIds = new HashSet<>();
        groupProvider.findAll().forEach(group -> {
            teamIdByPriority.put(group.priority(), group.groupName());
            validTeamIds.add(group.groupName());
        });

        // Remove teams that are not valid anymore
        scoreboard.getTeams().forEach(team -> {
            String teamId = team.getName();
            if (!validTeamIds.contains(teamId))
                team.unregister();
        });

        // Create a map of desired members by team
        Map<String, Set<String>> desiredMembersByTeam = new HashMap<>();
        playersBySortId.forEach((key, value) -> {
            String teamTagId = teamIdByPriority.get(key);
            if (teamTagId == null) return;
            desiredMembersByTeam.computeIfAbsent(teamTagId, k -> new HashSet<>()).addAll(value);
        });

        groupProvider.findAll().forEach(group -> {
            int groupId = group.id();
            int priority = group.priority();
            String teamTagId = group.groupName();

            Team team = existingTeams.get(teamTagId);
            if (team == null) team = scoreboard.registerNewTeam(teamTagId);

            GroupColor prefix = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_PREFIX.getId()).orElse(null);
            GroupColor suffix = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_SUFFIX.getId()).orElse(null);
            GroupColor color = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_COLOR.getId()).orElse(null);

            String formattedPrefix = prefix != null ? prefix.value() : permission.defaultTagPrefix();
            String formattedSuffix = suffix != null ? suffix.value() : permission.defaultTagSuffix();
            String formattedColor = color != null ? color.value() : permission.defaultTagColor();
            NamedTextColor textColor = NamedTextColor.NAMES.value(formattedColor.toLowerCase());

            if (!formattedPrefix.equals(miniMessage.serialize(team.prefix())))
                team.prefix(miniMessage.deserialize(permission.tagPrefixFormat(),
                        Placeholder.parsed("prefix", formattedPrefix)));
            if (!formattedSuffix.equals(miniMessage.serialize(team.suffix())))
                team.suffix(miniMessage.deserialize(permission.tagSuffixFormat(),
                        Placeholder.parsed("suffix", formattedSuffix)));
            if (textColor != null)
                team.color(textColor);

            List<String> playerNames = playersBySortId.get(priority);
            Set<String> desiredMembers = desiredMembersByTeam.getOrDefault(teamTagId, Collections.emptySet());
            Set<String> currentMembers = new HashSet<>(team.getEntries());

            if (playerNames != null)
                playerNames.forEach(team::addEntry);

            currentMembers.stream()
                    .filter(playerName -> !desiredMembers.contains(playerName))
                    .forEach(team::removeEntry);

            Team finalTeam = team;
            desiredMembers.stream()
                    .filter(playerName -> !finalTeam.hasEntry(playerName))
                    .forEach(team::addEntry);
        });
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (cacheName.equalsIgnoreCase(RefreshType.GROUP_COLORS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_GROUP_COLOR.getName())
                || cacheName.equalsIgnoreCase(RefreshType.GROUPS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_GROUP.getName())
                || cacheName.equalsIgnoreCase(RefreshType.USERS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_USER.getName())
                || cacheName.equalsIgnoreCase(RefreshType.PERMISSIONS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_PERMISSION.getName())
                || cacheName.equalsIgnoreCase(RefreshType.PARENTS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_PARENT.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SETTINGS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_SETTING.getName())
                || cacheName.equalsIgnoreCase(RefreshType.PREFIX_COLORS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_PREFIX_COLOR.getName())
                || cacheName.equalsIgnoreCase(RefreshType.USER_PREFIX_COLORS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_USER_PREFIX_COLOR.getName())
                || cacheName.equalsIgnoreCase(RefreshType.CLANS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_CLAN.getName())
                || cacheName.equalsIgnoreCase(RefreshType.CLAN_MEMBERS.getName())
                || cacheName.equalsIgnoreCase(RefreshType.SINGLE_CLAN_MEMBER.getName())
                || cacheName.equalsIgnoreCase(RefreshType.ALL.getName())) {
            hasUpdated.set(true);
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
    }

    public AtomicBoolean getHasUpdated() {
        return hasUpdated;
    }

    private @NotNull Map<Integer, TextColor> collectExplicitColorsByIndex(@NotNull Component input) {
        Map<Integer, TextColor> result = new HashMap<>();
        collectExplicitColorsByIndex(input, null, new int[]{0}, result);
        return result;
    }

    private void collectExplicitColorsByIndex(@NotNull Component input,
                                              TextColor inheritedColor,
                                              int @NotNull [] index,
                                              @NotNull Map<Integer, TextColor> out) {
        TextColor effectiveColor = input.color();
        if (effectiveColor == null)
            effectiveColor = inheritedColor;

        if (input instanceof TextComponent text && !text.content().isEmpty()) {
            String content = text.content();
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (effectiveColor != null && !Character.isWhitespace(c))
                    out.put(index[0], effectiveColor);
                index[0]++;
            }
        }

        for (Component child : input.children())
            collectExplicitColorsByIndex(child, effectiveColor, index, out);
    }

    private @NotNull Component applyExplicitColorsByIndex(@NotNull Component input,
                                                          @NotNull Map<Integer, TextColor> explicitColors) {
        return applyExplicitColorsByIndex(input, explicitColors, new int[]{0});
    }

    private @NotNull Component applyExplicitColorsByIndex(@NotNull Component input,
                                                          @NotNull Map<Integer, TextColor> explicitColors,
                                                          int @NotNull [] index) {
        Component updated = input;
        boolean replacedSelf = false;

        if (input instanceof TextComponent text && !text.content().isEmpty()) {
            Component rebuilt = Component.empty().style(text.style());
            String content = text.content();

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                Component character = Component.text(c).style(text.style());
                TextColor forcedColor = explicitColors.get(index[0]++);
                if (forcedColor != null)
                    character = character.color(forcedColor);
                rebuilt = rebuilt.append(character);
            }

            updated = rebuilt;
            replacedSelf = true;
        }

        List<Component> children = input.children();
        if (children.isEmpty())
            return updated;

        List<Component> updatedChildren = new ArrayList<>(children.size());
        for (Component child : children)
            updatedChildren.add(applyExplicitColorsByIndex(child, explicitColors, index));

        if (!replacedSelf)
            return updated.children(updatedChildren);

        Component merged = updated;
        for (Component child : updatedChildren)
            merged = merged.append(child);
        return merged;
    }
}
