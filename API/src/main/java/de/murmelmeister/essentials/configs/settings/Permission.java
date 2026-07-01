package de.murmelmeister.essentials.configs.settings;

import org.jetbrains.annotations.NotNull;

public record Permission(String chatFormat, String tabFormat,
                         String tagPrefixFormat, String tagSuffixFormat,
                         String defaultChatPrefix, String defaultChatSuffix, String defaultChatColor,
                         String defaultChatColorMessage,
                         String defaultTabPrefix, String defaultTabSuffix, String defaultTabColor,
                         String defaultTagPrefix, String defaultTagSuffix, String defaultTagColor) {
    public static @NotNull Permission defaults() {
        return new Permission(
                "<color><clan_sign><prefix><username><suffix><clan_tag><reset>",
                "<color><clan_sign><prefix><username><suffix><clan_tag>",
                "<prefix>",
                "<suffix>",
                "",
                "",
                "<gray>",
                " » ",
                "",
                "",
                "<gray>",
                "",
                "",
                "gray"
        );
    }
}
