package de.murmelmeister.essentials.manager.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CommandConfig {
    String id();

    String name();

    String[] aliases() default {};

    boolean bypass() default false;
}
