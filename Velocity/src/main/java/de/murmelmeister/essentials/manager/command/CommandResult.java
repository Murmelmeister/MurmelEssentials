package de.murmelmeister.essentials.manager.command;

public record CommandResult(int code, Integer rowsAffected, boolean log) {
    public static CommandResult of(int code) {
        return new CommandResult(code, null, true);
    }

    public static CommandResult of(int code, Integer rowsAffected) {
        return new CommandResult(code, rowsAffected, true);
    }

    public static CommandResult of(int code, boolean log) {
        return new CommandResult(code, null, log);
    }

    public static CommandResult of(int code, Integer rowsAffected, boolean log) {
        return new CommandResult(code, rowsAffected, log);
    }
}
