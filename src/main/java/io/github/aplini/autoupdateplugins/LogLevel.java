package io.github.aplini.autoupdateplugins;

import lombok.Getter;

@Getter
public enum LogLevel {
    DEBUG("", "DEBUG"),
    INFO("", "INFO"),
    MARK("§a", "MARK"),
    WARN("§e", "WARN"),
    NET_WARN("§e", "NET_WARN"),
    ;
    private final String color;
    private final String name;

    LogLevel(String color, String name) {
        this.color = color;
        this.name = name;
    }
}
