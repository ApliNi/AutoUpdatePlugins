package io.github.aplini.autoupdateplugins;

import io.github.aplini.autoupdateplugins.commands.CommandManager;
import io.github.aplini.autoupdateplugins.data.config.ConfigInstance;
import io.github.aplini.autoupdateplugins.data.config.ConfigManager;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import io.github.aplini.autoupdateplugins.data.temp.TempDataManager;
import io.github.aplini.autoupdateplugins.data.update.UpdateDataManager;
import io.github.aplini.autoupdateplugins.update.UpdateInstance;
import io.github.aplini.autoupdateplugins.utils.Util;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Objects;


public class AutoUpdate extends JavaPlugin {
    @Getter
    private final ConfigManager configManager = new ConfigManager(this);
    @Getter
    private final CommandManager commandManager;
    @Getter
    private final MessageManager messageManager;
    @Getter
    private final TempDataManager tempDataManager = new TempDataManager(this);
    @Getter
    private final UpdateDataManager updateDataManager;
    @Getter
    @Setter
    private UpdateInstance updateInstance;
    {
        try {
            messageManager = new MessageManager(configManager.getInstance().getLanguage(), this);
            updateDataManager = new UpdateDataManager(configManager.getInstance().getLanguage(), this);
            commandManager = new CommandManager(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Proxy proxy = Proxy.NO_PROXY;
        ConfigInstance.Proxy p = configManager.getInstance().getProxy();
        if(p.getType() != Proxy.Type.DIRECT) {
            proxy = new Proxy(p.getType(), new InetSocketAddress(p.getHost(), p.getPort()));
        }
        updateInstance = Util.getUpdateInstance(
                configManager.getInstance().getStartupDelay(),
                configManager.getInstance().getStartupCycle(),
                proxy,
                configManager.getInstance().getSetRequestProperty(),
                this,
                updateDataManager.getInstance().getList(),
                configManager.getInstance().getDownloadThreadCount(),
                configManager.getInstance().isSslVerify());
    }

    @Override
    public void onEnable() {
        Metrics metrics = new Metrics(this, 20629);
        metrics.addCustomChart(new Metrics.SingleLineChart("Plugins", () -> ((List<?>) Objects.requireNonNull(getConfig().get("list"))).size()));
        Objects.requireNonNull(Bukkit.getPluginCommand("aup")).setExecutor(commandManager);
    }

    @Override
    public void onDisable() {
        configManager.save();
        tempDataManager.save();
        updateInstance.stop();
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
