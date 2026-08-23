package de.murmelmeister.essentials.configs.settings;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public record Maintenance(List<Integer> whitelist) {
    public static @NotNull Maintenance defaults() {
        return new Maintenance(Collections.emptyList());
    }
}
