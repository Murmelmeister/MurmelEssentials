package de.murmelmeister.essentials.manager.command;

public record CommandResult(int code, Integer rowsAffected) {
    public static CommandResult of(int code) {
        return new CommandResult(code, null);
    }

    public static CommandResult of(int code, Integer rowsAffected) {
        return new CommandResult(code, rowsAffected);
    }
}
