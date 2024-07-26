package io.github.aplini.autoupdateplugins.data.config;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final AutoUpdate plugin;
    @Getter
    private ConfigInstance instance;

    public ConfigManager(AutoUpdate plugin) {
        this.plugin = plugin;
        plugin.reloadConfig();
        FileConfiguration reader = plugin.getConfig();
        instance = reader.getObject("", ConfigInstance.class, new ConfigInstance());
    }

    public void save() {
        AutoUpdate.getPlugin(AutoUpdate.class).getConfig().set("", instance);
        AutoUpdate.getPlugin(AutoUpdate.class).saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration reader = plugin.getConfig();
        instance = reader.getObject("", ConfigInstance.class, new ConfigInstance());
    }
}
