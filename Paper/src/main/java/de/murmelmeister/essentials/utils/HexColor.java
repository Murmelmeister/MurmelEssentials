package de.murmelmeister.essentials.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HexColor {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[A-Fa-f0-9]{6}");

    /**
     * Create HexColor and normal Color colors.
     *
     * @param message The message which should be in HexColor
     * @return The message that is made in the color
     */
    @SuppressWarnings("deprecation")
    public static String format(String message) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, String.valueOf(ChatColor.of(color)));
            matcher = HEX_COLOR_PATTERN.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
