package io.github.aplini.autoupdateplugins;

import io.github.aplini.autoupdateplugins.commands.CommandManager;
import io.github.aplini.autoupdateplugins.data.config.ConfigManager;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;


public class AutoUpdate extends JavaPlugin {
    @Getter
    public final ConfigManager configManager = new ConfigManager(this);
    @Getter
    public final CommandManager commandManager = new CommandManager(this);
    @Getter
    public final MessageManager messageManager;

    {
        try {
            messageManager = new MessageManager(configManager.getInstance().getLanguage(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        Objects.requireNonNull(Bukkit.getPluginCommand("aup")).setExecutor(commandManager);
    }

    @Override
    public void onDisable() {
        configManager.save();
    }

    @Override
    public void reloadConfig() {
        configManager.reload();
        messageManager.reload();
        commandManager.reload();
    }

    public void log(LogLevel level, String text) {
        if (configManager.getInstance().getLogLevel().contains(level.name())) {
            switch (level.getName()) {
                case "DEBUG", "INFO":
                    getLogger().info(text);
                    break;
                case "MARK":
                    // 一些新版本的控制台似乎很难显示颜色
                    Bukkit.getConsoleSender().sendMessage(level.getColor() + "[AUP] " + text);
                    break;
                case "WARN", "NET_WARN":
                    getLogger().warning(text);
                    break;
            }
        }
        //logList.add(level.color + (level.name.equals("INFO") ? "" : _fileName) + text);
    }
}
