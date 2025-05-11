package de.murmelmeister.essentials.utils;

public enum LegacyCode {
    BLACK('0', "black", 0x000000),
    DARK_BLUE('1', "dark_blue", 0x0000AA),
    DARK_GREEN('2', "dark_green", 0x00AA00),
    DARK_AQUA('3', "dark_aqua", 0x00AAAA),
    DARK_RED('4', "dark_red", 0xAA0000),
    DARK_PURPLE('5', "dark_purple", 0xAA00AA),
    GOLD('6', "gold", 0xFFAA00),
    GRAY('7', "gray", 0xAAAAAA),
    DARK_GRAY('8', "dark_gray", 0x555555),
    BLUE('9', "blue", 0x5555FF),
    GREEN('a', "green", 0x55FF55),
    AQUA('b', "aqua", 0x55FFFF),
    RED('c', "red", 0xFF5555),
    LIGHT_PURPLE('d', "light_purple", 0xFF55FF),
    YELLOW('e', "yellow", 0xFFFF55),
    WHITE('f', "white", 0xFFFFFF),
    MAGIC('k', "obfuscated"),
    BOLD('l', "bold"),
    STRIKETHROUGH('m', "strikethrough"),
    UNDERLINE('n', "underlined"),
    ITALIC('o', "italic"),
    RESET('r', "reset");
    public static final LegacyCode[] VALUES = values();

    private final char code;
    private final String name;
    private final Integer hex;

    LegacyCode(char code, String name, Integer hex) {
        this.code = code;
        this.name = name;
        this.hex = hex;
    }

    LegacyCode(char code, String name) {
        this(code, name, null);
    }

    public char getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getHex() {
        return hex;
    }

    public String getHexString() {
        return hex != null ? String.format("#%06X", hex) : null;
    }

    public static LegacyCode getByCode(char code) {
        for (LegacyCode legacy : VALUES)
            if (legacy.getCode() == code)
                return legacy;
        return null;
    }
}
