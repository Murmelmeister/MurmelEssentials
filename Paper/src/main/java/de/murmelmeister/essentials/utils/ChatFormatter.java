package de.murmelmeister.essentials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts permitted ampersand-based chat formatting into MiniMessage tags.
 *
 * <p>The following case-insensitive formats are supported:
 * <ul>
 *     <li>Legacy colors and decorations: {@code &cText&/c}</li>
 *     <li>Hex colors: {@code &#FF0000Text&/#FF0000}</li>
 *     <li>Gradients with at least two colors:
 *         {@code &g:#FF0000:#0000FF;Text&/g}</li>
 * </ul>
 *
 * <p>Each format requires its corresponding permission. Player-supplied
 * MiniMessage tags are escaped before conversion, so only the supported
 * ampersand syntax can introduce formatting. The fallback prefix is trusted
 * MiniMessage supplied by the plugin and is therefore not escaped.
 */
public final class ChatFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pattern LEGACY_COLOR_CODE = Pattern.compile("(?i)&(/?)([0-9a-fk-or])");
    private static final Pattern HEX_COLOR_CODE = Pattern.compile("(?i)&(/?)#([0-9a-f]{6})");
    private static final Pattern GRADIENT_COLOR_CODE = Pattern.compile("(?i)&g:(#[0-9a-f]{6}(?::#[0-9a-f]{6})+);");
    private static final Pattern GRADIENT_CLOSE_CODE = Pattern.compile("(?i)&/g");

    private static final String PERMISSION_COLOR_LEGACY = "murmel.color.legacy";
    private static final String PERMISSION_COLOR_HEX = "murmel.color.hex";
    private static final String PERMISSION_COLOR_GRADIENT = "murmel.color.gradient";

    private ChatFormatter() {
    }

    /**
     * Formats a raw player message according to the player's color permissions.
     *
     * <p>Unsupported or unauthorized ampersand codes remain unchanged. The
     * trusted {@code fallbackPrefix} is prepended before the complete message is
     * deserialized by MiniMessage.
     *
     * @param player         Player whose permissions control the available formats
     * @param raw            Untrusted message entered by the player
     * @param fallbackPrefix Trusted MiniMessage prefix providing the default style
     * @return The formatted chat component
     * @throws NullPointerException If any argument is {@code null}
     */
    public static Component format(Player player, String raw, String fallbackPrefix) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(fallbackPrefix, "fallbackPrefix");

        // Escape untrusted MiniMessage before introducing the permitted tags below.
        String safeRaw = MINI_MESSAGE.escapeTags(raw);

        if (player.hasPermission(PERMISSION_COLOR_GRADIENT))
            safeRaw = applyGradientColorCode(safeRaw);

        if (player.hasPermission(PERMISSION_COLOR_HEX))
            safeRaw = applyHexColorCode(safeRaw);

        if (player.hasPermission(PERMISSION_COLOR_LEGACY))
            safeRaw = applyLegacyColorCode(safeRaw);

        return MINI_MESSAGE.deserialize(fallbackPrefix + safeRaw);
    }

    /**
     * Converts gradient delimiters such as
     * {@code &g:#FF0000:#0000FF;} and {@code &/g} into their MiniMessage
     * equivalents.
     *
     * @param input Escaped message that may contain gradient codes
     * @return Message with converted gradient delimiters
     */
    private static String applyGradientColorCode(String input) {
        Matcher matcher = GRADIENT_COLOR_CODE.matcher(input);
        StringBuilder result = new StringBuilder(input.length());
        while (matcher.find())
            matcher.appendReplacement(result, Matcher.quoteReplacement("<gradient:" + matcher.group(1) + ">"));
        matcher.appendTail(result);
        return GRADIENT_CLOSE_CODE.matcher(result).replaceAll(Matcher.quoteReplacement("</gradient>"));
    }

    /**
     * Converts hexadecimal delimiters such as {@code &#FF0000} and
     * {@code &/#FF0000} into MiniMessage color tags.
     *
     * @param input Escaped message that may contain hexadecimal color codes
     * @return Message with converted hexadecimal color delimiters
     */
    private static String applyHexColorCode(String input) {
        Matcher matcher = HEX_COLOR_CODE.matcher(input);
        StringBuilder result = new StringBuilder(input.length());
        while (matcher.find()) {
            String tag = matcher.group(1).isEmpty() ? "<#" : "</#";
            matcher.appendReplacement(result, Matcher.quoteReplacement(tag + matcher.group(2) + ">"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Converts legacy color and decoration delimiters such as {@code &c} and
     * {@code &/l} into their named MiniMessage tags. Reset codes ({@code &r}
     * and {@code &/r}) both become {@code <reset>} because reset is not a
     * paired MiniMessage tag.
     *
     * @param input Escaped message that may contain legacy formatting codes
     * @return Message with converted legacy formatting delimiters
     */
    private static String applyLegacyColorCode(String input) {
        Matcher matcher = LEGACY_COLOR_CODE.matcher(input);
        StringBuilder result = new StringBuilder(input.length());
        while (matcher.find()) {
            char codeCharacter = Character.toLowerCase(matcher.group(2).charAt(0));
            LegacyCode code = LegacyCode.getByCode(codeCharacter);
            if (code == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            String replacement = codeCharacter == 'r'
                    ? "<reset>"
                    : (matcher.group(1).isEmpty() ? "<" : "</") + code.getName() + ">";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
