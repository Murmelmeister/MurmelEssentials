package de.murmelmeister.essentials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting chat messages by converting
 * legacy, hex, and gradient color codes into MiniMessage tags.
 *
 * <p>This class escapes user input, checks permissions, and then
 * applies regex-based transformations for supported color codes.
 */
public final class ChatFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pattern LEGACY_COLOR_CODE = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern HEX_COLOR_CODE = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern GRADIENT_COLOR_CODE = Pattern.compile("(?i)&g:(#[0-9A-F]{6}(?::#[0-9A-F]{6})*);");

    private static final String PERMISSION_COLOR_LEGACY = "murmel.color.legacy";
    private static final String PERMISSION_COLOR_HEX = "murmel.color.hex";
    private static final String PERMISSION_COLOR_GRADIENT = "murmel.color.gradient";

    /**
     * Formats the given raw message by escaping MiniMessage tags,
     * then applying to color-code transformations according to player permissions.
     *
     * @param player         The player whose permissions determine allowed codes
     * @param raw            The raw input string containing potential color codes
     * @param fallbackPrefix A MiniMessage prefix to prepend for default styling
     * @return A Component ready for display via MiniMessage
     */
    public static Component format(Player player, String raw, String fallbackPrefix) {
        // Escape any MiniMessage tags and quote replacement sequences
        String safeRaw = Matcher.quoteReplacement(MINI_MESSAGE.escapeTags(raw));

        // Apply gradient if permitted
        if (player.hasPermission(PERMISSION_COLOR_GRADIENT)
            && GRADIENT_COLOR_CODE.matcher(safeRaw).find())
            safeRaw = applyGradientColorCode(safeRaw);

        // Apply hex if permitted
        if (player.hasPermission(PERMISSION_COLOR_HEX)
            && HEX_COLOR_CODE.matcher(safeRaw).find())
            safeRaw = applyHexColorCode(safeRaw);

        // Apply legacy if permitted
        if (player.hasPermission(PERMISSION_COLOR_LEGACY)
            && LEGACY_COLOR_CODE.matcher(safeRaw).find())
            safeRaw = applyLegacyColorCode(safeRaw);

        // Deserialize into a MiniMessage component
        safeRaw = safeRaw.replace("\\$", "$");
        return MINI_MESSAGE.deserialize(fallbackPrefix + safeRaw);
    }

    /**
     * Replaces gradient color codes with MiniMessage <gradient> tags.
     *
     * @param input The string containing gradient codes
     * @return The transformed string with MiniMessage gradient tags
     */
    private static String applyGradientColorCode(String input) {
        Matcher matcher = GRADIENT_COLOR_CODE.matcher(input);
        while (matcher.find()) {
            String colors = input.substring(matcher.start(), matcher.end());
            input = input.replace(colors, "<gradient:" + colors.substring(3, colors.length() - 1) + ">")
                    .replaceFirst("&/g", "</gradient>");
            matcher = GRADIENT_COLOR_CODE.matcher(input);
        }
        return input;
    }

    /**
     * Replaces hex color codes with MiniMessage <#rrggbb> tags.
     *
     * @param input The string containing hex codes
     * @return The transformed string with MiniMessage hex tags
     */
    private static String applyHexColorCode(String input) {
        Matcher matcher = HEX_COLOR_CODE.matcher(input);
        while (matcher.find()) {
            String color = input.substring(matcher.start(), matcher.end());
            input = input.replace(color, "<#" + color.substring(2) + ">")
                    .replaceFirst("&/#" + color.substring(2), "</#" + color.substring(2) + ">");
            matcher = HEX_COLOR_CODE.matcher(input);
        }
        return input;
    }

    /**
     * Replaces legacy color and formatting codes with MiniMessage tags.
     *
     * @param input The string containing legacy codes
     * @return The transformed string with MiniMessage legacy tags
     */
    private static String applyLegacyColorCode(String input) {
        Matcher matcher = LEGACY_COLOR_CODE.matcher(input);
        while (matcher.find()) {
            String color = input.substring(matcher.start(), matcher.end());
            LegacyCode code = LegacyCode.getByCode(color.charAt(1));
            if (code == null) continue;
            input = input.replace(color, "<" + code.getName() + ">")
                    .replaceFirst("&/" + color.charAt(1), "</" + code.getName() + ">");
            matcher = LEGACY_COLOR_CODE.matcher(input);
        }
        return input;
    }
}
