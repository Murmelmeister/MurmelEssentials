package de.murmelmeister.essentials.utils;

public enum ConfigValue {
    VELOCITY_SUPPORT("velocity.support", true),
    PERMISSION_RANK_CHAT_ENABLE("permission.rank.chat.enable", true),
    PERMISSION_RANK_TAB_ENABLE("permission.rank.tab.enable", true),
    PERMISSION_RANK_TAG_ENABLE("permission.rank.tag.enable", true),
    PERMISSION_RANK_CHAT_FORMAT("permission.rank.chat.format", "<color><clan_sign><prefix><username><suffix><clan_tag><reset>"),
    PERMISSION_RANK_TAB_FORMAT("permission.rank.tab.format", "<color><clan_sign><prefix><username><suffix><clan_tag>"),
    PERMISSION_RANK_TAG_FORMAT_PREFIX("permission.rank.tag.format.prefix", "<clan_sign><prefix>"),
    PERMISSION_RANK_TAG_FORMAT_SUFFIX("permission.rank.tag.format.suffix", "<suffix><clan_tag>"),
    PERMISSION_RANK_CHAT_DEFAULT_PREFIX("permission.rank.chat.default.prefix", ""),
    PERMISSION_RANK_CHAT_DEFAULT_SUFFIX("permission.rank.chat.default.suffix", ""),
    PERMISSION_RANK_CHAT_DEFAULT_COLOR("permission.rank.chat.default.color", "<gray>"),
    PERMISSION_RANK_CHAT_DEFAULT_MESSAGE_FORMAT("permission.rank.chat.default.message_format", " <gray>» "),
    PERMISSION_RANK_TAB_DEFAULT_PREFIX("permission.rank.tab.default.prefix", ""),
    PERMISSION_RANK_TAB_DEFAULT_SUFFIX("permission.rank.tab.default.suffix", ""),
    PERMISSION_RANK_TAB_DEFAULT_COLOR("permission.rank.tab.default.color", "<gray>"),
    PERMISSION_RANK_TAG_DEFAULT_PREFIX("permission.rank.tag.default.prefix", ""),
    PERMISSION_RANK_TAG_DEFAULT_SUFFIX("permission.rank.tag.default.suffix", ""),
    PERMISSION_RANK_TAG_DEFAULT_COLOR("permission.rank.tag.default.color", "gray"),
    CHAT_PLAYER_HEAD_ENABLE("chat.player_head.enable", true),
    ;
    public static final ConfigValue[] VALUES = values();

    private final String path;
    private final Object defaultValue;

    ConfigValue(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
