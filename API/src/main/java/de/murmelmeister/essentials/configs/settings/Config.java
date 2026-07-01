package de.murmelmeister.essentials.configs.settings;

import org.jetbrains.annotations.NotNull;

public record Config(boolean autoUpdate, boolean tablistEnable, long tablistRefresh,
                     boolean prefixEnable) {
    public static @NotNull Config defaults() {
        return new Config(
                true,
                true,
                1000,
                true
        );
    }
}
