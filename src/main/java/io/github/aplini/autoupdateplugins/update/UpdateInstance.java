package io.github.aplini.autoupdateplugins.update;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.LogLevel;
import io.github.aplini.autoupdateplugins.beans.Github.GithubAPI;
import io.github.aplini.autoupdateplugins.beans.Github.GithubAsset;
import io.github.aplini.autoupdateplugins.beans.UpdateItem;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class UpdateInstance {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final _scheduleTask task;

    public UpdateInstance(int delay, int interval, OkHttpClient client, List<UpdateItem> items, AutoUpdate plugin, int poolSize) {
        task = new _scheduleTask(items, client, plugin, poolSize);
        scheduledExecutorService.scheduleAtFixedRate(task, delay, interval, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stop() {
        task.stop();
        scheduledExecutorService.shutdownNow();
    }

    private record _scheduleTask(List<UpdateItem> items, OkHttpClient client, AutoUpdate plugin,
                                 int poolSize) implements Runnable {
        static final AtomicBoolean isUpdating = new AtomicBoolean(false);
        static final Vector<UpdateItem> processed = new Vector<>();
        static ExecutorService executor;

        private _scheduleTask(List<UpdateItem> items, OkHttpClient client, AutoUpdate plugin, int poolSize) {
            this.items = items;
            this.client = client;
            this.plugin = plugin;
            this.poolSize = poolSize;
            executor = Executors.newFixedThreadPool(
                    poolSize,
                    new ThreadFactoryBuilder().setNameFormat("Update-Downloader-").build()
            );
        }

        @Override
        public void run() {
            if (!isUpdating.get()) {
                if (processed.size() == items.size()) {
                    processed.clear();
                    isUpdating.set(false);
                } else {
                    plugin.log(LogLevel.WARN, plugin.messageManager.getInstance().getUpdate().getErrStartRepeatedly());
                    return;
                }
            }
            isUpdating.set(true);
            plugin.log(LogLevel.WARN, plugin.messageManager.getInstance().getUpdate().getChecking());
            for (UpdateItem item : items)
                executor.submit(new _updateTask(item, client.newBuilder().build(), plugin.messageManager, plugin));
        }

        private void stop() {
            executor.shutdownNow();
        }

        private record _updateTask(UpdateItem item, OkHttpClient client,
                                   MessageManager messageManager, AutoUpdate plugin) implements Runnable {
            @Override
            public void run() {
                String url = item.getUrl().replaceAll("/$", "");
                URLType type = getUrlType(url);
                getDownloadUrl(type, url);
            }

            private String getDownloadUrl(URLType type, String url) {
                switch (type) {
                    case Github -> {
                        Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)$").matcher(url);
                        if(matcher.find()) {
                            try (Response resp = client.newCall(new Request.Builder().head().url(
                                    "https://api.github.com/repos" + matcher.group(0) + "/releases/latest"
                            ).build()).execute()) {
                                if(resp.code() != 200) {
                                    String[] split = matcher.group(0).split("\\\\");
                                    plugin.log(
                                            LogLevel.WARN,
                                            String.format(
                                                    "[%s][%s][%s]%s",
                                                    Thread.currentThread().getName(),
                                                    type.name(),
                                                    item.getFile(),
                                                    plugin.messageManager
                                                            .getInstance().getUpdate()
                                                            .getGithub()
                                                            .getRepoNotFound()
                                                            .replace("{owner}", split[0])
                                                            .replace("{repo}", split[1])
                                            ));
                                    return null;
                                }
                                GithubAPI[] unSerialized = new Gson().fromJson(Objects.requireNonNull(resp.body()).string(), GithubAPI[].class);
                                if(item.isGetPreRelease()) {
                                    for (GithubAsset asset : unSerialized[0].getAssets())
                                        if (item.getFileNamePattern().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(asset.getName()).matches()) {

                                            return asset.getUrl();
                                        }
                                } else {
                                    for(GithubAPI j : unSerialized) {
                                        if(!j.isPrerelease())
                                            for (GithubAsset asset : unSerialized[0].getAssets())
                                                if (item.getFileNamePattern().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(asset.getName()).matches()) {

                                                    return asset.getUrl();
                                                }
                                    }
                                }
                                return null;
                            } catch (IOException e) {
                                plugin.getLogger().log(Level.SEVERE,e.getMessage(),e);
                            }
                        }
                    }

                }
                return null;
            }

            @NotNull
            private URLType getUrlType(String url) {
                URLType type = URLType.Plain;
                if(url.contains("://github.com/")) type = URLType.Github;
                else if(url.contains("://www.spigotmc.org/")) type = URLType.Spigot;
                else if(url.contains("://modrinth.com/")) type = URLType.Modrinth;
                else if(url.contains("://dev.bukkit.org/")) type = URLType.Bukkit;
                else if(url.contains("://builds.guizhanss.com/")) type = URLType.GuiZhan;
                else if(url.contains("://www.minebbs.com/")) type = URLType.MineBBS;
                else if(url.contains("://legacy.curseforge.com/")) type = URLType.CurseForge;
                else {
                    try (Response resp = client.newCall(new Request.Builder().head().url(url).build()).execute()) {
                        Headers headers = resp.headers();
                        for (String key : headers.names())
                            if(key.equalsIgnoreCase("x-jenkins") || key.equalsIgnoreCase("x-hudson")) {
                                type = URLType.Jenkins;
                                break;
                            }
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE,e.getMessage(),e);
                    }
                }
                plugin.log(
                        LogLevel.DEBUG,
                        String.format(
                                "[%s][%s]%s",
                                Thread.currentThread().getName(),
                                item.getFile(),
                                plugin.messageManager
                                        .getInstance().getUpdate()
                                        .getSucceedGetType()
                                        .replace("{type}", type.name())
                        )
                );
                return type;
            }

            enum URLType {
                Github, Jenkins, CurseForge,
                Spigot, MineBBS, Modrinth,
                Bukkit, GuiZhan, Plain,
            }
        }
    }
}
