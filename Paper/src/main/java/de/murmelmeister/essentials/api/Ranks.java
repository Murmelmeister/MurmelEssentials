package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Ranks {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PERMISSION_CHAT_COLOR = "murmelessentials.chat.color";
    private static final String PERMISSION_CHAT_HEX = "murmelessentials.chat.hex";

    private static BukkitTask task;
    private static final ConcurrentMap<Player, Integer> counts = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Player, BukkitTask> animationTasks = new ConcurrentHashMap<>();
    private static final List<TextColor> textColors = Arrays.asList(
            TextColor.fromHexString("#ff00aa"),
            TextColor.fromHexString("#ee00aa"),
            TextColor.fromHexString("#dd00aa"),
            TextColor.fromHexString("#cc00aa"),
            TextColor.fromHexString("#bb00aa"),
            TextColor.fromHexString("#aa00aa"),
            TextColor.fromHexString("#9900aa"),
            TextColor.fromHexString("#8800aa"),
            TextColor.fromHexString("#7700aa"),
            TextColor.fromHexString("#6600aa"),
            TextColor.fromHexString("#5500aa"),
            TextColor.fromHexString("#4400aa"),
            TextColor.fromHexString("#3300aa"),
            TextColor.fromHexString("#2200aa"),
            TextColor.fromHexString("#1100aa"),
            TextColor.fromHexString("#0000aa"),
            TextColor.fromHexString("#1100aa"),
            TextColor.fromHexString("#2200aa"),
            TextColor.fromHexString("#3300aa"),
            TextColor.fromHexString("#4400aa"),
            TextColor.fromHexString("#5500aa"),
            TextColor.fromHexString("#6600aa"),
            TextColor.fromHexString("#7700aa"),
            TextColor.fromHexString("#8800aa"),
            TextColor.fromHexString("#9900aa"),
            TextColor.fromHexString("#aa00aa"),
            TextColor.fromHexString("#bb00aa"),
            TextColor.fromHexString("#cc00aa"),
            TextColor.fromHexString("#dd00aa"),
            TextColor.fromHexString("#ee00aa"),
            TextColor.fromHexString("#ff00aa")
    );
    private static final List<String> colors = Arrays.asList(
            "#ff00aa",
            "#ee00aa",
            "#dd00aa",
            "#cc00aa",
            "#bb00aa",
            "#aa00aa",
            "#9900aa",
            "#8800aa",
            "#7700aa",
            "#6600aa",
            "#5500aa",
            "#4400aa",
            "#3300aa",
            "#2200aa",
            "#1100aa",
            "#0000aa",
            "#0000aa",
            "#1100aa",
            "#2200aa",
            "#3300aa",
            "#4400aa",
            "#5500aa",
            "#6600aa",
            "#7700aa",
            "#8800aa",
            "#9900aa",
            "#aa00aa",
            "#bb00aa",
            "#cc00aa",
            "#dd00aa",
            "#ee00aa",
            "#ff00aa"
    );

    private static final Map<Integer, List<String>> animatedPrefixStarts = new HashMap<>();
    private static final Map<Integer, List<String>> animatedPrefixEnds = new HashMap<>();

    static {
        //animatedPrefixStarts.put(1, Arrays.asList("#ff0000", "#ffff00"));
        //animatedPrefixEnds.put(1, Arrays.asList("#ffff00", "#00ff00"));
    }

    /*public static void updatePlayers(MurmelEssentials instance, Server server) {
        if (task != null && task.isCancelled()) task.cancel();
        var hasUpdateOccurred = new AtomicBoolean(false);

        RefreshUtil.setRefreshListener(() -> hasUpdateOccurred.set(true));
        var group = instance.getGroup();
        var user = instance.getUser();

        task = server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            if (hasUpdateOccurred.get()) {
                for (var player : server.getOnlinePlayers()) {
                    setPlayerTeams(group, user, player);
                    setPlayerListName(group, user, player);
                    player.updateCommands(); // Update the player commands
                }
                hasUpdateOccurred.set(false);
            }
        }, 1L, 5L);
    }*/

    public void update(MurmelEssentials plugin, Player player) {
        var group = plugin.getGroup();
        var user = plugin.getUser();
        setPlayerTeams(group, user, player);
        setPlayerListName(group, user, player);
    }

    @SuppressWarnings("deprecation")
    public static void setChatFormat(AsyncChatEvent event, Group group, User user) {
        var player = event.getPlayer();
        //var idAndSortId = getUserIdAndSortId(group, user, player);
        var userId = user.getId(player.getUniqueId());
        var highestSortId = user.getParent().getHighestPriority(group, userId);
        var colorSettings = group.getColor();
        var chat = GroupColorType.CHAT;

        var serializer = LegacyComponentSerializer.builder().hexColors().build();
        var originalMessage = serializer.serialize(event.message());

        if (player.hasPermission(PERMISSION_CHAT_COLOR))
            originalMessage = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', originalMessage);
        if (player.hasPermission(PERMISSION_CHAT_HEX)) originalMessage = HexColor.format(originalMessage);

        final var finalMessage = originalMessage;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            var groupId = user.getParent().getParentIds(userId).stream()
                    .filter(id -> highestSortId == group.getPriority(id))
                    .findFirst();

            if (groupId.isPresent()) {
                int id = groupId.get();
                var format = colorSettings.getPrefix(id, chat) + player.getName() + colorSettings.getSuffix(id, chat) + " : ";
                var chatMessage = serializer.deserialize(HexColor.format(colorSettings.getColor(id, chat)) + finalMessage);

                return MINI_MESSAGE.deserialize(format).append(chatMessage);
            } else return message;
        });
    }

    private static void setPlayerListName(Group group, User user, Player player) {
        //var idAndSortId = getUserIdAndSortId(group, user, player);
        int userId = user.getId(player.getUniqueId());
        int highestSortId = user.getParent().getHighestPriority(group, userId);
        GroupColor colorSettings = group.getColor();
        GroupColorType tab = GroupColorType.TAB;
        boolean animated = false; // TODO: Get from database if the prefix is animated

        Optional<Integer> optionalGroupId = user.getParent().getParentIds(userId).stream()
                .filter(groupId -> highestSortId == group.getPriority(groupId))
                .findFirst();

        if (optionalGroupId.isPresent()) {
            int groupId = optionalGroupId.get();
            String prefix = colorSettings.getPrefix(groupId, tab);
            String color = colorSettings.getColor(groupId, tab);
            String suffix = colorSettings.getSuffix(groupId, tab);

            Component baseComponent = MINI_MESSAGE.deserialize(prefix + color + player.getName() + suffix);

            if (animated) {
                /*List<String> starts = animatedPrefixStarts.getOrDefault(groupId, Arrays.asList("#ff00aa", "#ee00aa", "#dd00aa", "#cc00aa", "#bb00aa", "#aa00a",
                        "#9900aa", "#8800aa", "#7700aa", "#6600aa", "#5500aa", "#4400aa", "#3300aa", "#2200aa", "#1100aa", "#0000aa"));
                List<String> ends = animatedPrefixEnds.getOrDefault(groupId, Arrays.asList("#0000aa", "#1100aa", "#2200aa", "#3300aa", "#4400aa", "#5500aa",
                        "#6600aa", "#7700aa", "#8800aa", "#9900aa", "#aa00aa", "#bb00aa", "#cc00aa", "#dd00aa", "#ee00aa", "#ff00aa"));*/

                List<TextColor> bigGradient = createMultiGradient(colors, 30);
                // System.out.println("bigGradient = " + bigGradient.size());

                if (bigGradient.isEmpty()) System.out.println("bigGradient is empty");
                else animateGradientLettersMulti(player, baseComponent, bigGradient, MurmelEssentials.getInstance());
                /*String animatedStartHex = getAnimatedPrefixStart(groupId);
                String animatedEndHex = getAnimatedPrefixEnd(groupId);
                TextColor startColor = TextColor.fromHexString(animatedStartHex);
                TextColor endColor = TextColor.fromHexString(animatedEndHex);*/

                //animateGradient(player, baseComponent, startColor, endColor, MurmelEssentials.getInstance());
            } else {
                player.playerListName(baseComponent);
            }
        }

        /*user.getParent().getParentIds(userId).stream()
                .filter(groupId -> highestSortId == group.getPriority(groupId))
                .forEach(groupId -> {
                    var playerListName = MINI_MESSAGE.deserialize(
                            colorSettings.getPrefix(groupId, tab) + colorSettings.getColor(groupId, tab) + player.getName() + colorSettings.getSuffix(groupId, tab));
                    player.playerListName(playerListName);
                });*/
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerTeams(Group group, User user, Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        GroupColorType tag = GroupColorType.TEAM;
        GroupColor colorSettings = group.getColor();

        synchronized (scoreboard) {
            //Map<String, Team> existingTeams = scoreboard.getTeams().stream().collect(Collectors.toMap(Team::getName, Function.identity()));
            Map<String, Team> existingTeams = new HashMap<>();
            for (Team team : scoreboard.getTeams())
                existingTeams.put(team.getName(), team);

            // Create a map of players and their highest SortID
            /*Map<Integer, List<String>> playersBySortId = player.getServer().getOnlinePlayers().stream()
                    .collect(Collectors.groupingBy(
                            target -> user.getParent().getHighestPriority(group, user.getId(target.getUniqueId())),
                            Collectors.mapping(Player::getName, Collectors.toList())
                    ));*/
            Map<Integer, List<String>> playersBySortId = new HashMap<>();
            for (Player target : player.getServer().getOnlinePlayers()) {
                int userId = user.getId(target.getUniqueId());
                int highestSortId = user.getParent().getHighestPriority(group, userId);
                playersBySortId.computeIfAbsent(highestSortId, k -> new ArrayList<>()).add(target.getName());
            }

            for (String groupName : group.getNames()) {
                int groupId = group.getUniqueId(groupName);
                int groupSortId = group.getPriority(groupId);
                String name = group.getTeamSort(groupId);

                Team team = existingTeams.get(name);
                if (team == null) team = scoreboard.registerNewTeam(name);

                String prefix = colorSettings.getPrefix(groupId, tag);
                String suffix = colorSettings.getSuffix(groupId, tag);
                String color = colorSettings.getColor(groupId, tag);

                if (prefix == null) prefix = "";
                if (suffix == null) suffix = "";
                if (color == null) color = "7";

                if (!prefix.equals(team.getPrefix())) team.prefix(MINI_MESSAGE.deserialize(prefix));
                if (!suffix.equals(team.getSuffix())) team.suffix(MINI_MESSAGE.deserialize(suffix));

                ChatColor chatColor = ChatColor.getByChar(color.replace("§", "").replace("&", ""));
                if (chatColor != null && !chatColor.equals(team.getColor()))
                    team.setColor(Objects.requireNonNull(chatColor));

                List<String> playerNames = playersBySortId.get(groupSortId);
                if (playerNames != null)
                    for (String playerName : playerNames)
                        team.addEntry(playerName);
            }
        }
    }

    private static void animateGradientLettersMulti(Player player, Component baseComponent, List<TextColor> colorList, Plugin plugin) {
        Component plainComponent = removeColors(baseComponent);
        String plainText = MINI_MESSAGE.serialize(plainComponent);

        cancelExistingTask(player);

        BukkitTask task = new BukkitRunnable() {

            @Override
            public void run() {
                List<TextColor> rotated = new ArrayList<>(colorList);
                Collections.rotate(rotated, 1);

                Component animatedName = Component.empty();
                int size = rotated.size();

                for (int i = 0; i < plainText.length(); i++) {
                    TextColor color = colorList.get(i % size);

                    animatedName = animatedName.append(
                            Component.text(String.valueOf(plainText.charAt(i))).color(color)
                    );
                }

                player.playerListName(animatedName);

                colorList.clear();
                colorList.addAll(rotated);
            }
        }.runTaskTimer(plugin, 0L, 2L);
        animationTasks.put(player, task);
    }

    private static List<TextColor> createMultiGradient(List<String> hexList, int stepsPerSegment) {
        if (hexList.size() < 2)
            throw new IllegalArgumentException("Hex list must contain at least two colors");

        List<TextColor> finalGradient = new ArrayList<>();

        for (int i = 0; i < hexList.size() - 1; i++) {
            List<TextColor> segment = createGradient(hexList.get(i), hexList.get(i + 1), stepsPerSegment);
            if (i > 0 && !segment.isEmpty()) segment.removeFirst();
            finalGradient.addAll(segment);
        }
        return finalGradient;
    }

    private static List<TextColor> createMultiGradient(List<String> startHexList, List<String> endHexList, int stepsPerSegment) {
        if (startHexList.size() != endHexList.size())
            throw new IllegalArgumentException("Start and end hex lists must be the same size");

        List<TextColor> finalGradient = new ArrayList<>();

        for (int i = 0; i < startHexList.size(); i++) {
            List<TextColor> segment = createGradient(startHexList.get(i), endHexList.get(i), stepsPerSegment);
            if (i > 0 && !segment.isEmpty()) segment.removeFirst();
            finalGradient.addAll(segment);
        }
        return finalGradient;
    }

    private static List<TextColor> createGradient(String startHexColor, String endHexColor, int steps) {
        Color startColor = Color.decode(startHexColor);
        Color endColor = Color.decode(endHexColor);
        List<TextColor> gradient = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (steps - 1);
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            gradient.add(TextColor.color(red, green, blue));
        }
        return gradient;
    }

    private static void animateGradientLetters(Player player, String playerName, Component prefixComponent, Component suffixComponent, List<TextColor> colorList, Plugin plugin) {
        cancelExistingTask(player);
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                Component animatedName = Component.empty();
                for (int i = 0; i < playerName.length(); i++) {
                    int index = (tick + i) % colorList.size();
                    TextColor letterColor = colorList.get(index);
                    animatedName = animatedName.append(Component.text(String.valueOf(playerName.charAt(i))).color(letterColor));
                }
                Component finalComponent = prefixComponent.append(animatedName).append(suffixComponent);
                player.playerListName(finalComponent);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        animationTasks.put(player, task);
    }

    /*public static String getAnimatedPrefixStart(int groupId) {
        return animatedPrefixStarts.getOrDefault(groupId, "#ffffff");
    }

    public static String getAnimatedPrefixEnd(int groupId) {
        return animatedPrefixEnds.getOrDefault(groupId, "#ffffff");
    }*/

    private static void animateGradient(Player player, Component component, TextColor startHexColor, TextColor endHexColor, Plugin plugin) {
        Component plainComponent = removeColors(component);
        String plainText = MINI_MESSAGE.serialize(plainComponent);
        //int length = plainText.length();
        //List<List<TextColor>> states = new ArrayList<>();
        List<TextColor> gradient = createGradient(startHexColor.asHexString(), endHexColor.asHexString(), 10);
        //List<List<TextColor>> patterns = createRotatingGradient(startHexColor, endHexColor, length);
        //final int totalSteps = gradient.size();

        cancelExistingTask(player);
        //counts.put(player, 0);

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                Component finalComponent = Component.empty();
                for (int i = 0; i < plainText.length(); i++) {
                    int colorIndex = (tick + i) % gradient.size();
                    TextColor letterColor = gradient.get(colorIndex);
                    finalComponent = finalComponent.append(Component.text(String.valueOf(plainText.charAt(i))).color(letterColor));
                }
                /*for (int i = 0; i < pattern.size(); i++) {
                    Collections.rotate(pattern, i);
                    states.add(pattern);
                }*/

                //List<TextColor> currentPattern = createGradient(startHexColor, endHexColor, length, offset);
                //TextComponent animatedComponent = formatGradientText(plainText, currentGradient);

                player.playerListName(finalComponent);

                /*counts.put(player, counts.get(player) + 1);
                if (counts.get(player) >= pattern.size()) counts.put(player, 0);*/
                //offset = (offset + 1) % totalSteps;
                //counts.put(player, offset);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        animationTasks.put(player, task);
    }

    private static Component removeColors(Component component) {
        if (component instanceof TextComponent textComponent) {
            TextComponent newComponent = Component.text(textComponent.content());

            for (Component child : textComponent.children())
                newComponent = newComponent.append(removeColors(child));
            return newComponent;
        }
        return component;
    }

    public static void cancelExistingTask(Player player) {
        if (animationTasks.containsKey(player)) {
            BukkitTask existingTask = animationTasks.get(player);
            existingTask.cancel();
            animationTasks.remove(player);
            counts.remove(player);
        }
        BukkitTask existingTask = animationTasks.get(player);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
            animationTasks.remove(player);
            counts.remove(player);
        }
    }

    private static TextComponent formatGradientText(String input, List<TextColor> gradient) {
        TextComponent component = Component.empty();
        char[] chars = input.toCharArray();
        int count = 0;

        for (char character : chars) {
            if (count >= gradient.size()) count = 0;
            component = component.append(Component.text(character).color(gradient.get(count)));
            count++;
        }
        return component;
    }

    /*private static Pair<Integer, Integer> getUserIdAndSortId(Group group, User user, Player player) {
        var userId = user.getId(player.getUniqueId());
        var highestSortId = getHighestSortId(group, user, userId);
        return new ImmutablePair<>(userId, highestSortId);
    }

    private static int getHighestSortId(Group group, User user, int userId) {
        return user.getParent().getParentIds(userId).stream()
                .map(group::getPriority)
                .collect(Collectors.summarizingInt(Integer::intValue))
                .getMax();
    }*/

    /*

    private static List<TextColor> createGradient(String startHexColor, String endHexColor, int steps, float offset) {
        Color startColor = Color.decode(startHexColor);
        Color endColor = Color.decode(endHexColor);
        List<TextColor> gradient = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            float ratio = ((float) i / (steps - 1) + offset) % 1.0f;
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            gradient.add(TextColor.color(red, green, blue));
        }
        return gradient;
    }

    private static List<List<TextColor>> createRotatingGradient(String startHexColor, String endHexColor, int steps) {
        Color startColor = Color.decode(startHexColor);
        Color endColor = Color.decode(endHexColor);
        List<TextColor> gradient = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (steps - 1);
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            gradient.add(TextColor.color(red, green, blue));
        }

        List<List<TextColor>> rotatingGradient = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            Collections.rotate(gradient, 1);
            rotatingGradient.add(new ArrayList<>(gradient));
        }
        return rotatingGradient;
    }

    private static TextComponent formatGradientText(String input, String startHexColor, String endHexColor, int steps) {
        TextComponent component = Component.empty();
        List<TextColor> gradient = createGradient(startHexColor, endHexColor, steps);
        char[] chars = input.toCharArray();

        int count = 0;
        for (char character : chars) {
            if (count >= gradient.size()) count = 0;
            component = component.append(Component.text(character).color(gradient.get(count)));
            count++;
        }
        return component;
    }

    private static TextComponent applyGradientToComponent(Component component, String startHexColor, String endHexColor, int steps) {
        Component plainComponent = removeColors(component);
        String plainText = MINI_MESSAGE.serialize(plainComponent);
        return formatGradientText(plainText, startHexColor, endHexColor, steps);
    }

    private static List<TextColor> createGradient(TextColor startColor, TextColor endColor, int steps) {
        List<TextColor> gradient = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (steps - 1);

            int red = (int) (startColor.red() + ratio * (endColor.red() - startColor.red()));
            int green = (int) (startColor.green() + ratio * (endColor.green() - startColor.green()));
            int blue = (int) (startColor.blue() + ratio * (endColor.blue() - startColor.blue()));

            gradient.add(TextColor.color(red, green, blue));
        }
        return gradient;
    }

    private static void animatedPlayerListName(Player player, Plugin plugin, Component component) {
        String plainText = MINI_MESSAGE.serialize(removeColors(component));
        List<List<TextColor>> states = new ArrayList<>();
        List<TextColor> patterns = textColors;

        if (animationTasks.containsKey(player)) cancelExistingTask(player);
        counts.put(player, 0);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (int i = 0; i < patterns.size(); i++) {
                Collections.rotate(patterns, i);
                states.add(patterns);
            }

            TextComponent color = formatGradientText(plainText, states.get(counts.get(player)));
            player.playerListName(color);

            counts.put(player, counts.get(player) + 1);
            if (counts.get(player) >= states.size()) counts.put(player, 0);
        }, 0L, 2L);
        animationTasks.put(player, task);
    }

    private static List<TextColor> filterOddIndexes(List<TextColor> colors) {
        List<TextColor> result = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            if (i % 2 != 0) result.add(colors.get(i));
        }
        return result;
    }

    public static void startPlayerAnimation(Player player, Plugin plugin, int number, Component component) {
        String plainText = MINI_MESSAGE.serialize(removeColors(component));
        List<TextColor> colorsToAnimate = number % 2 == 0 ? textColors : filterOddIndexes(textColors);
        long delay = number % 2 == 0 ? 5L : 20L;
        if (animationTasks.containsKey(player)) animationTasks.get(player).cancel();
        BukkitTask animationTask = new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= colorsToAnimate.size()) count = 0;
                player.sendMessage(formatGradientText(plainText, colorsToAnimate.get(count)));
            }
        }.runTaskTimer(plugin, 0L, delay);
        animationTasks.put(player, animationTask);
    }*/
}
