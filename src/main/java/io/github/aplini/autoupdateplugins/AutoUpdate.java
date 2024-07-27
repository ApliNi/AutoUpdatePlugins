package io.github.aplini.autoupdateplugins;

import io.github.aplini.autoupdateplugins.commands.CommandManager;
import io.github.aplini.autoupdateplugins.data.config.ConfigInstance;
import io.github.aplini.autoupdateplugins.data.config.ConfigManager;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import io.github.aplini.autoupdateplugins.data.temp.TempDataManager;
import io.github.aplini.autoupdateplugins.data.update.UpdateDataManager;
import io.github.aplini.autoupdateplugins.update.UpdateInstance;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
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
        updateInstance = new UpdateInstance(
                configManager.getInstance().getStartupDelay(),
                configManager.getInstance().getStartupCycle(),
                new OkHttpClient.Builder()
                        .proxy(proxy)
                        .addInterceptor(new Interceptor() {
                            @NotNull
                            @Override
                            public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                                Headers.Builder headers = new Headers.Builder();
                                for (ConfigInstance.Header header : configManager.getInstance().getSetRequestProperty())
                                    headers.add(header.getName(),header.getValue());
                                return chain.proceed(
                                        chain.request().newBuilder()
                                                .headers(headers.build())
                                                .build()
                                );
                            }
                        })
                        .build(),
                updateDataManager.getInstance().getList(),
                this,
                configManager.getInstance().getDownloadThreadCount()
                );
    }

    @Override
    public void onEnable() {
        Objects.requireNonNull(Bukkit.getPluginCommand("aup")).setExecutor(commandManager);
    }

    @Override
    public void onDisable() {
        configManager.save();
        tempDataManager.save();
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
