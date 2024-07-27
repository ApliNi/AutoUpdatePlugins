package io.github.aplini.autoupdateplugins.update;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.LogLevel;
import io.github.aplini.autoupdateplugins.beans.CurseForge.CurseForgeData;
import io.github.aplini.autoupdateplugins.beans.Github.GithubAPI;
import io.github.aplini.autoupdateplugins.beans.Github.GithubAsset;
import io.github.aplini.autoupdateplugins.beans.Jenkins.JenkinsAPI;
import io.github.aplini.autoupdateplugins.beans.Jenkins.JenkinsArtifact;
import io.github.aplini.autoupdateplugins.beans.Modrinth.ModrinthAPI;
import io.github.aplini.autoupdateplugins.beans.Modrinth.ModrinthFiles;
import io.github.aplini.autoupdateplugins.beans.TempData;
import io.github.aplini.autoupdateplugins.beans.UpdateItem;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import io.github.aplini.autoupdateplugins.utils.Util;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.aplini.autoupdateplugins.utils.Util.*;

@Getter
public class UpdateInstance {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final _scheduleTask task;
    private final AutoUpdate plugin;
    public UpdateInstance(int delay, int interval, OkHttpClient client, List<UpdateItem> items, AutoUpdate plugin, int poolSize) {
        this.plugin = plugin;
        task = new _scheduleTask(items, client, plugin, poolSize);
        scheduledExecutorService.scheduleAtFixedRate(task, delay, interval, java.util.concurrent.TimeUnit.SECONDS);
    }

    public boolean stop() {
        scheduledExecutorService.shutdownNow();
        return task.stop() && scheduledExecutorService.isShutdown();
    }
    public void run(CommandSender sender) {
        if(task.isUpdating.get()) {
            Util.Message(sender, plugin.getMessageManager().getInstance().getUpdate().getErrStartRepeatedly());
            return;
        }
        scheduledExecutorService.schedule(task,0, TimeUnit.SECONDS);
    }

    private class  _scheduleTask implements Runnable {
        final List<UpdateItem> items;
        final OkHttpClient client;
        final AutoUpdate plugin;
        final int poolSize;
        final AtomicBoolean isUpdating = new AtomicBoolean(false);
        @Getter
        final ConcurrentHashMap<UpdateItem, Boolean> processed = new ConcurrentHashMap<>();
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
                    plugin.log(LogLevel.WARN, plugin.getMessageManager().getInstance().getUpdate().getErrStartRepeatedly());
                    return;
                }
            }
            isUpdating.set(true);
            plugin.log(LogLevel.WARN, plugin.getMessageManager().getInstance().getUpdate().getChecking());
            for (UpdateItem item : items)
                executor.submit(new _updateTask(item, client.newBuilder().build(), plugin.getMessageManager(), plugin, processed));
        }

        private boolean stop() {
            executor.shutdownNow();
            return executor.isShutdown();
        }

        private record _updateTask(UpdateItem item, OkHttpClient client,
                                   MessageManager messageManager, AutoUpdate plugin,
                                   ConcurrentHashMap<UpdateItem, Boolean> processed
                                   ) implements Runnable {
            @Override
            public void run() {
                Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                    if(e instanceof InterruptedException){
                        processed.put(item,false);
                        return;
                    }
                    plugin.getLogger().log(Level.WARNING,String.format("[%s][%s]",
                            Thread.currentThread().getName(),
                            item.getFile()
                    ) + e.getMessage(), e);
                    processed.put(item,false);
                });
                String _updatePath, _filePath,_tempPath;
                if(!item.getFile().isEmpty() || !item.getUrl().isEmpty()) {
                    plugin.log(LogLevel.WARN, plugin.getMessageManager().getInstance().getUpdate().getListConfigErrMissing());
                    processed.put(item,false);
                    return;
                }
                Matcher tempMatcher = Pattern.compile("(.*/|.*\\\\)([^/\\\\]+)$").matcher(item.getFile());
                if(tempMatcher.find()){
                    getPath(tempMatcher.group(1));
                    _updatePath = item.getFile();
                    _filePath = item.getFile();
                    _tempPath = getPath(plugin.getConfigManager().getInstance().getPaths().getTempPath()) + tempMatcher.group(2);
                } else if (item.getPath() != null) {
                    _updatePath = getPath(item.getPath()+item.getFile());
                    _filePath = _updatePath;
                    _tempPath = getPath(plugin.getConfigManager().getInstance().getPaths().getTempPath()) + item.getFile();
                } else {
                    _updatePath = getPath(getNonNullString(item.getUpdatePath(),plugin.getConfigManager().getInstance().getPaths().getUpdatePath(),""));
                    _filePath = getPath(getNonNullString(item.getFilePath(),plugin.getConfigManager().getInstance().getPaths().getFilePath(),""));
                    _tempPath = getPath(getNonNullString(item.getTempPath(),plugin.getConfigManager().getInstance().getPaths().getTempPath(),""));
                }
                String url = item.getUrl().replaceAll("/$", "");
                URLType type = getUrlType(url);
                long newFileSize;
                try {
                    String downloadURL = getDownloadUrl(type, url);
                    if(downloadURL == null) {
                        plugin.log(
                                LogLevel.WARN,
                                String.format(
                                        "[%s][%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        type.name(),
                                        item.getFile(),
                                        plugin.getMessageManager()
                                                .getInstance().getUpdate()
                                                .getNoFileMatching()
                                ));
                        processed.put(item,false);
                        return;
                    }
                    try (Response resp = client.newCall(new Request.Builder().url(downloadURL).get().build()).execute()) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            throw new IOException("Failed to download file: " + item.getFile());
                        }
                        newFileSize = Long.getLong(resp.header("Content-Length"), -1);
                        try (
                                InputStream is = resp.body().byteStream();
                                OutputStream os = new FileOutputStream(_tempPath)
                        ) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                        if(
                                (plugin.getConfigManager().getInstance().isZipFileCheck() || item.isZipFileCheck())
                                        && Pattern.compile(
                                        plugin.getConfigManager().getInstance().getZipFileCheckPattern()
                                ).matcher(item.getFile()).find()
                        ) {
                            if(isJARFileIntact(_tempPath)) {
                                new File(_tempPath).delete();
                                plugin.log(
                                        LogLevel.DEBUG,
                                        String.format(
                                                "[%s][%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                type.name(),
                                                item.getFile(),
                                                plugin.getMessageManager().getInstance().getUpdate().getZipFileCheck()
                                        ));
                                processed.put(item,false);
                                return;
                            }
                        }
                        if(plugin.getConfigManager().getInstance().isEnablePreviousUpdate()) {
                            String sha1 = calculateSHA1(new File(_tempPath));
                            plugin.getTempDataManager().getTempDataMap().put(
                                    item.getFile(),
                                    new TempData(
                                            item.getFile(),
                                            System.currentTimeMillis(),
                                            downloadURL,
                                            sha1,
                                            newFileSize
                                    )
                            );
                        }
                    } catch (IOException e) {
                        new File(_tempPath).delete();
                        throw e;
                    }
                } catch (IOException | NoSuchAlgorithmException e) {
                    plugin.getLogger().log(Level.WARNING,String.format("[%s][%s][%s]",
                            Thread.currentThread().getName(),
                            type.name(),
                            item.getFile()
                    ) + e.getMessage(), e);
                    processed.put(item,false);
                    return;
                }
                if(plugin.getConfigManager().getInstance().isIgnoreDuplicates() && item.isIgnoreDuplicates()) {
                    try {
                        String tmpSHA1 = calculateSHA1(new File(_tempPath));
                        String originSHA1 = calculateSHA1(new File(_updatePath));
                        if (tmpSHA1.equalsIgnoreCase(originSHA1)) {
                            plugin.log(
                                    LogLevel.DEBUG,
                                    String.format(
                                        "[%s][%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        type.name(),
                                        item.getFile(),plugin.getMessageManager().getInstance().getUpdate().getTempAlreadyLatest()
                            ));
                            processed.put(item,false);
                            return;
                        }
                    } catch (IOException | NoSuchAlgorithmException ignore) {
                        //IGNORE
                    }
                    long oldFileSize = new File(_updatePath).exists() ? new File(_updatePath).length() : new File(_filePath).length();
                    try {
                        Files.move(Path.of(_tempPath), Path.of(_updatePath), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
                        processed.put(item,false);
                        return;
                    }
                    plugin.log(LogLevel.DEBUG,
                            String.format(
                                    "[%s][%s][%s]%s",
                                    Thread.currentThread().getName(),
                                    type.name(),
                                    item.getFile(),plugin.getMessageManager().getInstance().getUpdate().getFileSizeDifference()
                                            .replace("{old}",Long.toString(oldFileSize))
                                            .replace("{new}",Long.toString(newFileSize))
                            ));
                    processed.put(item,true);
                }
            }

            private String getDownloadUrl(URLType type, String url) throws IOException {
                switch (type) {
                    case Github -> {
                        Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)$").matcher(url);
                        if(matcher.find()) {
                            try (Response resp = client.newCall(new Request.Builder().head().url(
                                    "https://api.github.com/repos" + matcher.group(0) + "/releases/latest"
                            ).build()).execute()) {
                                if(resp.code() != 200 || resp.body() == null) {
                                    String[] split = matcher.group(0).split("\\\\");
                                    plugin.log(
                                            LogLevel.WARN,
                                            String.format(
                                                    "[%s][%s][%s]%s",
                                                    Thread.currentThread().getName(),
                                                    type.name(),
                                                    item.getFile(),
                                                    plugin.getMessageManager()
                                                            .getInstance().getUpdate()
                                                            .getGithub()
                                                            .getRepoNotFound()
                                                            .replace("{owner}", split[0])
                                                            .replace("{repo}", split[1])
                                            ));
                                    return null;
                                }
                                GithubAPI[] deSerialized = new Gson().fromJson(resp.body().string(), GithubAPI[].class);
                                if(item.isGetPreRelease()) {
                                    for (GithubAsset asset : deSerialized[0].getAssets())
                                        if (item.getFileNamePattern().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(asset.getName()).matches()) {
                                            plugin.log(LogLevel.DEBUG, String.format(
                                                    "[%s][%s]%s",
                                                    Thread.currentThread().getName(),
                                                    item.getFile(),
                                                    plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                                            .replace("{url}", asset.getUrl())
                                            ));
                                            return asset.getUrl();
                                        }
                                } else {
                                    for(GithubAPI j : deSerialized) {
                                        if(!j.isPrerelease())
                                            for (GithubAsset asset : j.getAssets())
                                                if (item.getFileNamePattern().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(asset.getName()).matches()) {
                                                    plugin.log(LogLevel.DEBUG, String.format(
                                                            "[%s][%s]%s",
                                                            Thread.currentThread().getName(),
                                                            item.getFile(),
                                                            plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                                                    .replace("{url}", asset.getUrl())
                                                    ));
                                                    return asset.getUrl();
                                                }
                                    }
                                }
                                plugin.log(LogLevel.WARN, String.format(
                                        "[%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        item.getFile(),
                                        plugin.getMessageManager().getInstance().getUpdate().getNoFileMatching()
                                ));
                                return null;
                            }
                        }
                        break;
                    }
                    case Jenkins -> {
                        try (Response resp = client.newCall(new Request.Builder().head().url(
                                url + "/lastSuccessfulBuild/api/json"
                        ).build()).execute()) {
                            if(resp.code() != 200 || resp.body() == null) {
                                plugin.log(
                                        LogLevel.WARN,
                                        String.format(
                                                "[%s][%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                type.name(),
                                                item.getFile(),
                                                plugin.getMessageManager()
                                                        .getInstance().getUpdate()
                                                        .getResourceNotFound()
                                        ));
                                return null;
                            }
                            JenkinsAPI deSerialized = new Gson().fromJson(Objects.requireNonNull(resp.body()).string(), JenkinsAPI.class);
                            for (JenkinsArtifact artifact : deSerialized.getArtifacts()) {
                                if (item.getFileNamePattern().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(artifact.getFileName()).matches()) {
                                    plugin.log(LogLevel.DEBUG, String.format(
                                            "[%s][%s]%s",
                                            Thread.currentThread().getName(),
                                            item.getFile(),
                                            plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                                    .replace("{url}", url +"/lastSuccessfulBuild/artifact/"+ artifact.getRelativePath())
                                    ));
                                    return url +"/lastSuccessfulBuild/artifact/"+ artifact.getRelativePath();
                                }
                            }
                            plugin.log(
                                    LogLevel.WARN,
                                    String.format(
                                            "[%s][%s][%s]%s",
                                            Thread.currentThread().getName(),
                                            type.name(),
                                            item.getFile(),
                                            plugin.getMessageManager()
                                                    .getInstance().getUpdate()
                                                    .getNoFileMatching()
                                    ));
                            return null;
                        }
                    }
                    case Spigot -> {
                        Matcher matcher = Pattern.compile("([0-9]+)$").matcher(url);
                        if(matcher.find()){
                            String dUrl = "https://api.spiget.org/v2/resources/"+ matcher.group(1) +"/download";
                            plugin.log(LogLevel.DEBUG, String.format(
                                    "[%s][%s]%s",
                                    Thread.currentThread().getName(),
                                    item.getFile(),
                                    plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                            .replace("{url}", dUrl)
                            ));
                            return dUrl;
                        }
                        plugin.log(
                                LogLevel.WARN,
                                String.format(
                                        "[%s][%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        type.name(),
                                        item.getFile(),
                                        plugin.getMessageManager()
                                                .getInstance().getUpdate()
                                                .getResourceNotFound()
                                ));
                        return null;
                    }
                    case Modrinth -> {
                        Matcher matcher = Pattern.compile("/([^/]+)$").matcher(url);
                        if(matcher.find()) {
                            try (Response resp = client.newCall(new Request.Builder().head().url(
                                    "https://api.modrinth.com/v2/project"+ matcher.group(0) +"/version"
                            ).build()).execute()) {
                                if (resp.code() != 200 || resp.body() == null) {
                                    plugin.log(
                                            LogLevel.WARN,
                                            String.format(
                                                    "[%s][%s][%s]%s",
                                                    Thread.currentThread().getName(),
                                                    type.name(),
                                                    item.getFile(),
                                                    plugin.getMessageManager()
                                                            .getInstance().getUpdate()
                                                            .getResourceNotFound()
                                            ));
                                    return null;
                                }
                                ModrinthAPI deSerialized = new Gson().fromJson(Objects.requireNonNull(resp.body()).string(), ModrinthAPI.class);
                                for (ModrinthFiles file : deSerialized.getFiles()) {
                                    if(file.getFilename().isEmpty() || Pattern.compile(item.getFileNamePattern()).matcher(file.getFilename()).matches()){
                                        plugin.log(LogLevel.DEBUG, String.format(
                                                "[%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                item.getFile(),
                                                plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                                        .replace("{url}", file.getUrl())
                                        ));
                                        return file.getUrl();
                                    }
                                }
                                plugin.log(
                                        LogLevel.WARN,
                                        String.format(
                                                "[%s][%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                type.name(),
                                                item.getFile(),
                                                plugin.getMessageManager()
                                                        .getInstance().getUpdate()
                                                        .getNoFileMatching()
                                        ));
                                return null;
                            }
                        }
                        plugin.log(
                                LogLevel.WARN,
                                String.format(
                                        "[%s][%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        type.name(),
                                        item.getFile(),
                                        plugin.getMessageManager()
                                                .getInstance().getUpdate()
                                                .getResourceNotFound()
                                ));
                        return null;
                    }
                    case Bukkit -> {
                        plugin.log(LogLevel.DEBUG, String.format(
                                "[%s][%s]%s",
                                Thread.currentThread().getName(),
                                item.getFile(),
                                plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                        .replace("{url}", url +"/files/latest")
                        ));
                        return url +"/files/latest";
                    }
                    case GuiZhan -> {
                        Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)$").matcher(url);
                        plugin.log(LogLevel.DEBUG, String.format(
                                "[%s][%s]%s",
                                Thread.currentThread().getName(),
                                item.getFile(),
                                plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                        .replace("{url}", "https://builds.guizhanss.com/api/download"+ matcher.group(0) +"/latest")
                        ));
                        return "https://builds.guizhanss.com/api/download"+ matcher.group(0) +"/latest";
                    }
                    case MineBBS -> {
                        plugin.log(LogLevel.DEBUG, String.format(
                                "[%s][%s]%s",
                                Thread.currentThread().getName(),
                                item.getFile(),
                                plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                        .replace("{url}", url + "/download")
                        ));
                        return url + "/download";
                    }
                    case CurseForge -> {
                        try (Response htmlResp = client.newCall(new Request.Builder().head().url(url).build()).execute()) {
                            if (htmlResp.code() != 200 || htmlResp.body() == null) {
                                plugin.log(
                                        LogLevel.WARN,
                                        String.format(
                                                "[%s][%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                type.name(),
                                                item.getFile(),
                                                plugin.getMessageManager()
                                                        .getInstance().getUpdate()
                                                        .getResourceNotFound()
                                        ));
                                return null;
                            }
                            String[] lines = htmlResp.body().string().split("<a");
                            for(String li : lines){
                                Matcher matcher = Pattern.compile("data-project-id=\"([0-9]+)\"").matcher(li);
                                if(matcher.find()) {
                                    try (Response resp = client.newCall(new Request.Builder().head().url("https://api.curseforge.com/servermods/files?projectIds="+ matcher.group(1)).build()).execute()) {
                                        if (resp.code() != 200 || resp.body() == null) {
                                            plugin.log(
                                                    LogLevel.WARN,
                                                    String.format(
                                                            "[%s][%s][%s]%s",
                                                            Thread.currentThread().getName(),
                                                            type.name(),
                                                            item.getFile(),
                                                            plugin.getMessageManager()
                                                                    .getInstance().getUpdate()
                                                                    .getResourceNotFound()
                                                    ));
                                            return null;
                                        }
                                        CurseForgeData[] data = new Gson().fromJson(resp.body().string(), CurseForgeData[].class);
                                        String durl = data[data.length - 1].getFileUrl();
                                        if(!item.isGetPreRelease()) {
                                            for (int i = data.length - 2; i >= 0; i--) {
                                                if (data[i].getGameVersion().equalsIgnoreCase("release")) {
                                                    durl = data[i].getFileUrl();
                                                    break;
                                                }
                                            }
                                        }
                                        plugin.log(LogLevel.DEBUG, String.format(
                                                "[%s][%s]%s",
                                                Thread.currentThread().getName(),
                                                item.getFile(),
                                                plugin.getMessageManager().getInstance().getUpdate().getFindDownloadUrl()
                                                        .replace("{url}", durl)
                                        ));
                                        return durl;
                                    }
                                }
                            }
                        }
                        plugin.log(
                                LogLevel.WARN,
                                String.format(
                                        "[%s][%s][%s]%s",
                                        Thread.currentThread().getName(),
                                        type.name(),
                                        item.getFile(),
                                        plugin.getMessageManager()
                                                .getInstance().getUpdate()
                                                .getNoFileMatching()
                                ));
                        return null;
                    }
                    case Plain -> {
                        return url;
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
                                plugin.getMessageManager()
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
