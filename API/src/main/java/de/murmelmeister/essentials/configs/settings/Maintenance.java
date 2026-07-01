package de.murmelmeister.essentials.configs.settings;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public record Maintenance(boolean mode, String motd, int protocolVersion, String protocolName,
                          List<Integer> whitelist) {
    public static @NotNull Maintenance defaults() {
        return new Maintenance(
                false,
                "<#00ffFF>MurmelAPI <#454545>| <#ff0000>Maintenance\n<#ff00bb>Please try again later.",
                0,
                "Maintenance",
                Collections.emptyList()
        );
    }
}
