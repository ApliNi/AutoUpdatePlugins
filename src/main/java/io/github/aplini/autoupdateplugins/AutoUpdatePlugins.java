package io.github.aplini.autoupdateplugins;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;


public final class AutoUpdatePlugins extends JavaPlugin implements Listener, CommandExecutor, TabExecutor {
    // 防止重复运行更新
    boolean lock = false;
    // 等待更新完成后再重载配置
    boolean awaitReload = false;
    // 计时器对象
    Timer timer = null;
    // 更新处理线程
    CompletableFuture<Void> future = null;
    // 记录最后一个使用指令的对象
    CommandSender lastSender = null;

    File tempFile;
    FileConfiguration temp;

    List<String> logList = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("aup")).setExecutor(this);

        // bStats
        if(getConfig().getBoolean("bStats", true)){
            new Metrics(this, 20629);
        }

        // 禁用证书验证
        if(getConfig().getBoolean("disableCertificateVerification", false)) {
            // 创建一个 TrustManager, 它将接受任何证书
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };
            // 获取默认的 SSLContext
            SSLContext sslContext;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 设置默认的 SSLSocketFactory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }
    }


    public void saveDate(){
        try {
            temp.save(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @EventHandler // 服务器启动完成事件
    public void onServerLoad(ServerLoadEvent event) {
        // 异步
        CompletableFuture.runAsync(this::setTimer);

        // 检查过时的配置
        if(getConfig().getBoolean("debugLog", false)){
            getLogger().warning("`debugLog` 配置已弃用, 请使用 `logLevel` - 启用哪些日志等级");
        }

        // 检查缺失的配置
        if(getConfig().get("setRequestProperty") == null){
            getLogger().warning("缺少配置 `setRequestProperty` - HTTP 请求中编辑请求头");
        }
        if(getConfig().get("message") == null){
            getLogger().warning("缺少配置 `message` - 插件消息配置");
        }
    }

    public void loadConfig(){
        // 导出不同语言的配置文件
        List<String> locales = List.of("config_en.yml");
        getPath("./plugins/AutoUpdatePlugins/Locales");
        for(String li : locales){
            File file = new File("./plugins/AutoUpdatePlugins/Locales/"+ li);
            if(file.exists()){
                continue;
            }
            try {
                file.createNewFile();
                ByteStreams.copy(
                        Objects.requireNonNull(getResource("Locales/"+ li)),
                        new FileOutputStream(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 加载语言和配置
        reloadConfig();
        loadMessage();

        tempFile = new File("./plugins/AutoUpdatePlugins/temp.yml");
        temp = YamlConfiguration.loadConfiguration(tempFile);
        if(temp.get("previous") == null){
            temp.set("previous", new HashMap<>());
        }
        saveDate();
    }
    public void setTimer(){
        long startupDelay = getConfig().getLong("startupDelay", 64);
        long startupCycle = getConfig().getLong("startupCycle", 61200);
        // 检查更新间隔是否过低
        if(startupCycle < 256 && !getConfig().getBoolean("disableUpdateCheckIntervalTooLow", false)){
            getLogger().warning(m.updateCheckIntervalTooLow);
            startupCycle = 512;
        }
        // 计时器
        getLogger().info(m.piece(m.timer, startupDelay, startupCycle));
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new updatePlugins(), startupDelay * 1000, startupCycle * 1000);
    }

    @Override // 指令补全
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return  List.of(
                    "reload",   // 重载插件
                    "update",   // 运行更新
                    "log",      // 查看日志
                    "stop"      // 立即停止当前更新
            );
        }
        return null;
    }
    @Override // 运行指令
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        lastSender = sender;

        // 默认输出插件信息
        if(args.length == 0){
            sender.sendMessage("""
                    IpacEL > AutoUpdatePlugins: 自动更新插件
                      指令:
                        - /aup reload - 重载配置
                        - /aup update - 运行更新
                        - /aup log    - 查看完整日志
                        - /aup stop   - 停止当前更新""");
            return true;
        }

        // 重载配置
        else if(args[0].equals("reload")){
            if(lock){
                awaitReload = true;
                sender.sendMessage("[AUP] "+ m.commandReloadOnUpdating);
                return true;
            }
            loadConfig();
            sender.sendMessage("[AUP] "+ m.commandReloadOK);
            setTimer();
            return true;
        }

        // 手动运行更新
        else if(args[0].equals("update")){
            if(lock && !getConfig().getBoolean("disableLook", false)){
                sender.sendMessage("[AUP] "+ m.commandRepeatedRunUpdate);
                return true;
            }
            sender.sendMessage("[AUP] "+ m.commandUpdateStart);
            new Timer().schedule(new updatePlugins(), 0);
            return true;
        }

        // 查看日志
        else if(args[0].equals("log")){
            sender.sendMessage("[AUP] "+ m.commandFullLog);
            for(String li : logList){
                sender.sendMessage("  | " + li);
            }
            return true;
        }

        // 立即停止当前更新
        else if(args[0].equals("stop")){
            if(lock){
                future.cancel(true);
                sender.sendMessage("[AUP] "+ m.commandStopUpdateIng);
            }else{
                sender.sendMessage("[AUP] "+ m.stopUpdate);
            }
        }
        return false;
    }


    private class updatePlugins extends TimerTask {
        String _nowFile = "[???] ";     // 当前文件的名称
        String _nowParser = "[???] ";   // 用于解析直链的解析器名称
        int _fail = 0;              // 更新失败数量
        int _success = 0;           // 更新成功数量
        int _allRequests = 0;       // 共进行的网络请求数量
        long _startTime;            // 最终耗时
        float _allFileSize = 0;     // 已下载的文件大小合计

        // 在这里存放当前插件的配置
        String c_file;              // 文件名称
        String c_url;               // 下载链接
        String c_tempPath;          // 下载缓存路径, 默认使用全局配置
        String c_updatePath;        // 更新存放路径, 默认使用全局配置
        String c_filePath;          // 最终安装路径, 默认使用全局配置
        String c_get;               // 查找单个文件的正则表达式, 默认选择第一个. 仅限 GitHub, Jenkins, Modrinth
        boolean c_zipFileCheck;     // 启用 zip 文件完整性检查, 默认 true
        boolean c_getPreRelease;    // 允许下载预发布版本, 默认 false. 仅限 GitHub

        public void run() {
            // 新线程
            future = CompletableFuture.runAsync(() -> {
                // 防止重复运行
                if(lock && !getConfig().getBoolean("disableLook", false)){
                    log(logLevel.WARN, m.repeatedRunUpdate);
                    return;
                }
                lock = true;
                logList = new ArrayList<>();    // 清空上一份日志
                _startTime = System.nanoTime(); // 记录运行时间

                log(logLevel.INFO, m.updateStart);

                List<?> list = (List<?>) getConfig().get("list");
                if(list == null){
                    log(logLevel.WARN, m.configErrList);
                    return;
                }

                for(Object _li : list){
                    _fail ++;
                    if(future.isCancelled()){
                        log(logLevel.INFO, m.stopUpdate);
                        if(lastSender != null && lastSender instanceof Player){
                            lastSender.sendMessage("[AUP] "+ m.stopUpdate);
                        }
                        return;
                    }

                    Map<?, ?> li = (Map<?, ?>) _li;
                    if(li == null){
                        log(logLevel.WARN, m.configErrUpdate);
                        continue;
                    }

                    // 检查基础配置
                    c_file = (String) SEL(li.get("file"), "");
                    c_url = ((String) SEL(li.get("url"), "")).trim();
                    if(c_file.isEmpty() || c_url.isEmpty()){
                        log(logLevel.WARN, m.configErrMissing);
                        continue;
                    }

                    _nowFile = "["+ c_file +"] "; // 用于显示日志的插件名称
                    c_tempPath = getPath(getConfig().getString("tempPath", "./plugins/AutoUpdatePlugins/temp/")) + c_file;

                    // 每个单独的配置
                    c_updatePath = getPath((String) SEL(li.get("updatePath"), getConfig().getString("updatePath", "./plugins/update/"))) + c_file;
                    c_filePath = getPath((String) SEL(li.get("filePath"), getConfig().getString("filePath", "./plugins/"))) + c_file;
                    if(li.get("path") != null){
                        c_updatePath = getPath((String) li.get("path")) + c_file;
                        c_filePath = c_updatePath;
                    }
                    c_get = (String) SEL(li.get("get"), "");
                    c_zipFileCheck = (boolean) SEL(li.get("zipFileCheck"), true);
                    c_getPreRelease = (boolean) SEL(li.get("getPreRelease"), false);

                    // 下载文件到缓存目录
                    log(logLevel.DEBUG, m.updateChecking);
                    String dUrl = getFileUrl(c_url, c_get);
                    if(dUrl == null){
                        log(logLevel.WARN, _nowFile + _nowParser + m.updateErrParsingDUrl);
                        continue;
                    }
                    dUrl = checkURL(dUrl);
//                    outInfo(dUrl);

                    // 启用上一个更新记录与检查
                    String feature = "";
                    String pPath = "";
                    if(getConfig().getBoolean("enablePreviousUpdate", true)){
                        // 获取文件大小
                        feature = getFeature(dUrl);
                        // 是否与上一个版本相同
                        pPath = "previous." + li.toString().hashCode();
                        if (temp.get(pPath) != null) {
                            // 检查数据差异
                            if(temp.getString(pPath + ".dUrl", "").equals(dUrl) &&
                                    temp.getString(pPath + ".feature", "").equals(feature)){
                                log(logLevel.MARK, m.updateTempAlreadyLatest);
                                _fail--;
                                continue;
                            }
                        }
                    }

                    if(!downloadFile(dUrl, c_tempPath)){
                        log(logLevel.WARN, m.updateErrDownload);
                        delFile(c_tempPath);
                        continue;
                    }

                    // 记录文件大小
                    float fileSize = new File(c_tempPath).length();
                    _allFileSize += fileSize;

                    // 文件完整性检查
                    if(c_zipFileCheck && !isJARFileIntact(c_tempPath)){
                        log(logLevel.WARN, m.updateZipFileCheck);
                        delFile(c_tempPath);
                        continue;
                    }

                    // 此时已确保文件(信息)正常
                    if(getConfig().getBoolean("enablePreviousUpdate", true)){
                        // 更新数据
                        temp.set(pPath + ".file", c_file);
                        temp.set(pPath + ".time", nowDate());
                        temp.set(pPath + ".dUrl", dUrl);
                        temp.set(pPath + ".feature", feature);
                    }

                    // 在这里实现运行系统命令的功能

                    // 哈希值检查, 如果新文件哈希与更新目录中的相等, 或者与正在运行的版本相等, 则无需更新
                    if(getConfig().getBoolean("ignoreDuplicates", true) && (boolean) SEL(li.get("ignoreDuplicates"), true)){
                        String updatePathFileHas = fileHash(c_updatePath);
                        String tempFileHas = fileHash(c_tempPath);
                        if(Objects.equals(tempFileHas, updatePathFileHas) || Objects.equals(tempFileHas, fileHash(c_filePath))){
                            log(logLevel.MARK, m.updateFileAlreadyLatest);
                            _fail --;
                            delFile(c_tempPath);
                            continue;
                        }
                    }

                    // 获取旧版本的文件大小, 优先在更新目录中查找, 没有再查找最终安装位置. 如果文件均不存在会返回 0
                    float oldFileSize = new File(c_updatePath).exists() ? new File(c_updatePath).length() : new File(c_filePath).length();

                    // 移动到更新目录
                    try {
                        Files.move(Path.of(c_tempPath), Path.of(c_updatePath), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log(logLevel.WARN, e.getMessage());
                    }

                    // 更新完成, 并显示文件大小变化
                    log(logLevel.DEBUG, m.piece(m.updateFulSizeDifference, String.format("%.2f", oldFileSize / 1048576), String.format("%.2f", fileSize / 1048576)));
                    _success ++;

                    _nowFile = "[???] ";
                    _nowParser = "[???] ";
                    _fail --;
                }

                saveDate();

                log(logLevel.INFO, m.updateFul);
                log(logLevel.INFO, "  - "+ m.piece(m.updateFulTime, Math.round((System.nanoTime() - _startTime) / 1_000_000_000.0)));

                String st = "  - ";
                if(_fail != 0){st += m.piece(m.updateFulFail, _fail);}
                if(_success != 0){st += m.piece(m.updateFulUpdate, _success);}
                log(logLevel.INFO, st + m.piece(m.updateFulOK, list.size()));

                log(logLevel.INFO, "  - "+ m.piece(m.updateFulNetRequest, _allRequests) + m.piece(m.updateFulDownloadFile, String.format("%.2f", _allFileSize / 1048576)));

                // 运行被推迟的配置重载
                if(awaitReload){
                    awaitReload = false;
                    loadConfig();
                    getLogger().info("[AUP] "+ m.logReloadOK);
                    setTimer();
                }
            });

            // 在任务完成后执行的代码
            future.thenRun(() -> lock = false);
        }

        // 尝试打开 jar 文件以判断文件是否完整
        public boolean isJARFileIntact(String filePath) {
            // 是否启用完整性检查
            if(getConfig().getBoolean("zipFileCheck", true)){
                try {
                    JarFile jarFile = new JarFile(new File(filePath));
                    jarFile.close();
                    return true;
                } catch (ZipException e) { // 文件不完整
                    return false;
                } catch (Exception e) { // 其他异常
                    return false;
                }
            }else{
                return true;
            }
        }

        // 计算文件哈希
        public String fileHash(String filePath) {
            try {
                byte[] data = Files.readAllBytes(Paths.get(filePath));
                byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                return new BigInteger(1, hash).toString(16);
            } catch (Exception e) {
//                outInfo(logLevel.WARN, e.getMessage()); // 文件不存在时会输出异常
            }
            return "null";
        }

        // 获取部分文件直链
        public  String getFileUrl(String _url, String matchFileName) {
            // 移除 URL 最后的斜杠
            String url = _url.replaceAll("/$", "");

            if(url.contains("://github.com/")){ // GitHub 发布
                _nowParser = "[GitHub] ";
                // 获取路径 "/ApliNi/Chat2QQ"
                Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)$").matcher(url);
                if(matcher.find()){
                    String data;
                    Map<?, ?> map;
                    // 是否允许下载预发布
                    if(c_getPreRelease){
                        // 获取所有发布中的第一个版本
                        data = httpGet("https://api.github.com/repos" + matcher.group(0) + "/releases");
                        if(data == null){return null;}
                        map = (Map<?, ?>) new Gson().fromJson(data, ArrayList.class).get(0);
                    }else{
                        // 获取一个最新版本
                        data = httpGet("https://api.github.com/repos" + matcher.group(0) + "/releases/latest");
                        if(data == null){return null;}
                        map = new Gson().fromJson(data, HashMap.class);
                    }
                    // 遍历发布文件列表
                    ArrayList<?> assets = (ArrayList<?>) map.get("assets");
                    for(Object _li : assets){
                        Map<?, ?> li = (Map<?, ?>) _li;
                        String fileName = (String) li.get("name");
                        if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                            String dUrl = (String) li.get("browser_download_url");
                            log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                            return dUrl;
                        }
                    }
                    log(logLevel.WARN, "[GitHub] "+ m.piece(m.debugNoFileMatching, url));
                    return null;
                }
                log(logLevel.WARN, "[GitHub] "+ m.piece(m.debugNoRepositoryPath, url));
                return null;
            }

            else if(url.contains("://ci.")){ // Jenkins
                _nowParser = "[Jenkins] ";
                // https://ci.viaversion.com/view/ViaBackwards/job/ViaBackwards-DEV/lastSuccessfulBuild/artifact/build/libs/ViaBackwards-4.10.0-23w51b-SNAPSHOT.jar
                String data = httpGet(url +"/lastSuccessfulBuild/api/json");
                if(data == null){return null;}
                Map<?, ?> map = new Gson().fromJson(data, HashMap.class);
                ArrayList<?> artifacts = (ArrayList<?>) map.get("artifacts");
                // 遍历发布文件列表
                for(Object _li : artifacts){
                    Map<?, ?> li = (Map<?, ?> ) _li;
                    String fileName = (String) li.get("fileName");
                    if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                        String dUrl = url +"/lastSuccessfulBuild/artifact/"+ li.get("relativePath");
                        log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                        return dUrl;
                    }
                }
                log(logLevel.WARN, "[Jenkins] "+ m.piece(m.debugNoFileMatching, url));
                return null;
            }

            else if(url.contains("://www.spigotmc.org/")){ // Spigot 页面
                _nowParser = "[Spigot] ";
                // 获取插件 ID
                Matcher matcher = Pattern.compile("([0-9]+)$").matcher(url);
                if(matcher.find()){
                    String dUrl = "https://api.spiget.org/v2/resources/"+ matcher.group(1) +"/download";
                    log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                    return dUrl;
                }
                log(logLevel.WARN, "[Spigot] "+ m.piece(m.debugErrUrlResolveNoID, url));
                return null;
            }

            else if(url.contains("://modrinth.com/")){ // Modrinth 页面
                _nowParser = "[Modrinth] ";
                Matcher matcher = Pattern.compile("/([^/]+)$").matcher(url);
                if(matcher.find()) {
                    String data = httpGet("https://api.modrinth.com/v2/project"+ matcher.group(0) +"/version");
                    if(data == null){return null;}
                    // 0 为最新的一个版本
                    Map<?, ?> map = (Map<?, ?>) ((ArrayList<?>) new Gson().fromJson(data, ArrayList.class)).get(0);
                    ArrayList<?> files = (ArrayList<?>) map.get("files");

                    // 遍历发布文件列表
                    for(Object _li : files){
                        Map<?, ?> li = (Map<?, ?>) _li;
                        String fileName = (String) li.get("filename");
                        if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                            String dUrl = (String) li.get("url");
                            log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                            return dUrl;
                        }
                    }
                    log(logLevel.WARN, "[Modrinth] "+ m.piece(m.debugNoFileMatching, url));
                    return null;
                }
                log(logLevel.WARN, "[Modrinth] "+ m.piece(m.debugErrUrlResolveNoName, url));
                return null;
            }

            else if(url.contains("://dev.bukkit.org/")){ // Bukkit 页面
                _nowParser = "[Bukkit] ";
                String dUrl = url +"/files/latest";
                log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                return dUrl;
            }

            else if(url.contains("://builds.guizhanss.com/")){ // 鬼斩构建站
                _nowParser = "[鬼斩构建站] ";
                // https://builds.guizhanss.com/SlimefunGuguProject/AlchimiaVitae/master

                // 获取路径 "/ApliNi/plugin/master"
                Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)$").matcher(url);
                if(matcher.find()){
                    // 获取所有发布中的第一个版本
                    String data = httpGet("https://builds.guizhanss.com/api/builds" + matcher.group(0));
                    if(data == null){return null;}
                    ArrayList<?> arr = (ArrayList<?>) new Gson().fromJson(data, HashMap.class).get("data");
                    Map<?, ?> map = (Map<?, ?>) arr.get(arr.size() - 1); // 获取最后一项

                    String dUrl = "https://builds.guizhanss.com/r2"+ matcher.group(0) +"/"+ map.get("target");
                    log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                    return dUrl;
                }
                log(logLevel.WARN, _nowParser + m.piece(m.debugNoRepositoryPath, url));
                return null;
            }

            else if(url.contains("://legacy.curseforge.com/")){ // CurseForge 页面
                _nowParser = "[CurseForge] ";
                // https://legacy.curseforge.com/minecraft/bukkit-plugins/dynmap
                // data-project-id="31620"

                String html = httpGet(url); // 下载 html 网页, 获取 project-id
                if(html == null){return null;}
                Matcher matcher = Pattern.compile("data-project-id=\"([0-9+])\"").matcher(html);
                if(matcher.find()){
                    String data = httpGet(matcher.group(1));
                    if(data == null){return null;}
                    ArrayList<?> arr = (ArrayList<?>) new Gson().fromJson(data, ArrayList.class);
                    Map<?, ?> map = (Map<?, ?>) arr.get(arr.size() - 1); // 获取最后一项
                    String dUrl = (String) map.get("downloadUrl");
                    log(logLevel.DEBUG, _nowParser + m.piece(m.debugGetVersion, dUrl));
                    return dUrl;
                }
                log(logLevel.WARN, _nowParser + m.piece(m.debugErrNoID, url));
                return null;
            }

            else{ // 没有匹配的项
                _nowParser = "[URL] ";
                log(logLevel.DEBUG, _nowParser + _url);
                return _url;
            }
        }

        // 如果 in1 为空则选择 in2, 否则选择 in1
        public Object SEL(Object in1, Object in2) {
            if(in1 == null){
                return in2;
            }
            return in1;
        }

        // http 请求获取字符串
        public String httpGet(String url) {
            HttpURLConnection cxn = getHttpCxn(url);
            if(cxn == null){return null;}
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(cxn.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    stringBuilder.append(line);
                }
                reader.close();
                cxn.disconnect();
                return String.valueOf(stringBuilder);
            } catch (Exception e) {
                log(logLevel.NET_WARN, "[HTTP] "+ e.getMessage());
            }
            cxn.disconnect();
            return null;
        }

        // 下载文件到指定位置, 并使用指定文件名
        public boolean downloadFile(String url, String path){
            delFile(path); // 删除可能存在的旧文件
            HttpURLConnection cxn = getHttpCxn(url);
            if(cxn == null){return false;}
            try {
                BufferedInputStream in = new BufferedInputStream(cxn.getInputStream());
                Path savePath = Path.of(path);
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                cxn.disconnect();
                return true;
            } catch (Exception e) {
                log(logLevel.NET_WARN, "[HTTP] "+ e.getMessage());
            }
            cxn.disconnect();
            return false;
        }

        // 通过 HEAD 请求获取一些特征信息
        public String getFeature(String url){
            String out = "_"+ nowDate().hashCode();
            try {
                HttpURLConnection cxn = (HttpURLConnection) new URI(url).toURL().openConnection();
                cxn.setRequestMethod("HEAD");
                _allRequests ++;

                int cl = cxn.getContentLength();
                String lh = cxn.getHeaderField("Location");

                if(cl != -1) {
                    out = "cl_"+ cl;
                }
                else if(lh != null){
                    out = "lh_"+ lh.hashCode();
                }
                cxn.disconnect();
            } catch (Exception e) {
                log(logLevel.NET_WARN, "[HTTP.HEAD] "+ e.getMessage());
            }
            return out;
        }

        // 获取 HTTP 连接
        public HttpURLConnection getHttpCxn(String url){
            HttpURLConnection cxn = null;
            try {
                cxn = (HttpURLConnection) new URI(url).toURL().openConnection();
                cxn.setRequestMethod("GET");
                _allRequests ++;
                // 填充请求头数据
                List<?> list = (List<?>) getConfig().get("setRequestProperty");
                if(list != null){
                    for(Object _li : list) {
                        Map<?, ?> li = (Map<?, ?>) _li;
                        cxn.setRequestProperty((String) li.get("name"), (String) li.get("value"));
                    }
                }
                if(cxn.getResponseCode() == 200){
                    return cxn;
                }
                cxn.disconnect();
                log(logLevel.NET_WARN, "[HTTP]"+ m.piece(m.httpRequestFailed, cxn.getResponseCode(), url));
            } catch (Exception e) {
                log(logLevel.NET_WARN, "[HTTP] "+ e.getMessage());
            }
            if(cxn != null){cxn.disconnect();}
            return null;
        }

        // 在插件更新过程中输出尽可能详细的日志
        public void log(logLevel level, String text){

            // 获取用户启用了哪些日志等级
            List<String> userLogLevel = getConfig().getStringList("logLevel");
            if(userLogLevel.isEmpty()){
                userLogLevel = List.of("DEBUG", "MARK", "INFO", "WARN", "NET_WARN");
            }

            if(userLogLevel.contains(level.name)){
                switch(level.name){
                    case "DEBUG":
                        getLogger().info(_nowFile + text);
                        break;
                    case "INFO":
                        getLogger().info(text);
                        break;
                    case "MARK":
                        // 一些新版本的控制台似乎很难显示颜色
                        Bukkit.getConsoleSender().sendMessage(level.color +"[AUP] "+ _nowFile + text);
                        break;
                    case "WARN", "NET_WARN":
                        getLogger().warning(_nowFile + text);
                        break;
                }
            }

            // 根据日志等级添加样式代码, 并记录到 logList
            // 非 INFO 日志添加 _nowFile 文本
            logList.add(level.color + (level.name.equals("INFO") ? "" : _nowFile) +  text);
        }
        enum logLevel {
            // 允许被忽略的 INFO
            DEBUG("", "DEBUG"),
            // 不可被忽略的 INFO
            INFO("", "INFO"),
            // 用于标记任务完成
            MARK("§a", "MARK"),
            // 警告
            WARN("§e", "WARN"),
            // 网络请求模式警告
            NET_WARN("§e", "NET_WARN"),
            ;
            private final String color;
            private final String name;
            logLevel(String color, String name) {
                this.color = color;
                this.name = name;
            }
        }

        // 获取已格式化的时间
        public String nowDate(){
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return now.format(formatter);
        }

        // 处理 URL 中的特殊字符
        public String checkURL(String url){
            // 清除前后的空格
            // 转义 URL 中的空格
            try {
                return new URI(url.trim()
                        .replace(" ", "%20"))
                        .toASCIIString();
            } catch (URISyntaxException e) {
                log(logLevel.WARN, "[URI] "+ m.piece(m.urlInvalid, url));
                return null;
            }
        }

        // 删除文件
        public void delFile(String path){
            new File(path).delete();
            // outInfo(logLevel.WARN, _nowFile +"[FILE] 删除文件失败: "+ path);
        }
    }

    // 创建目录
    public String getPath(String path) {
        Path directory = Paths.get(path);
        try {
            Files.createDirectories(directory);
            return directory + "/";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 显示消息
    public static class m {
        public static String updateCheckIntervalTooLow;
        public static String timer;
        public static String commandReloadOnUpdating;
        public static String commandReloadOK;
        public static String commandRepeatedRunUpdate;
        public static String commandUpdateStart;
        public static String commandFullLog;
        public static String commandStopUpdateIng;
        public static String stopUpdate;
        public static String repeatedRunUpdate;
        public static String updateStart;
        public static String configErrList;
        public static String configErrUpdate;
        public static String configErrMissing;
        public static String updateChecking;
        public static String updateErrParsingDUrl;
        public static String updateTempAlreadyLatest;
        public static String updateErrDownload;
        public static String updateZipFileCheck;
        public static String updateFileAlreadyLatest;
        public static String updateFulSizeDifference;
        public static String updateFul;
        public static String updateFulTime;
        public static String updateFulFail;
        public static String updateFulUpdate;
        public static String updateFulOK;
        public static String updateFulNetRequest;
        public static String updateFulDownloadFile;
        public static String logReloadOK;
        public static String debugGetVersion;
        public static String debugNoFileMatching;
        public static String debugNoRepositoryPath;
        public static String debugErrUrlResolveNoID;
        public static String debugErrUrlResolveNoName;
        public static String debugErrNoID;
        public static String httpRequestFailed;
        public static String urlInvalid;

        // 处理消息模板
        public static String piece(String message, Object in1){return message.replace("%1", ""+ in1);}
        public static String piece(String message, Object in1, Object in2){return piece(message, in1).replace("%2", ""+ in2);}
//        public static String piece(String message, Object in1, Object in2, Object in3){return piece(message, in1, in2).replace("%3", ""+ in3);}
    }

    public String gm(String key, String _default){
        return getConfig().getString("message."+ key, _default);
    }
    public void loadMessage(){
        m.updateCheckIntervalTooLow = gm("updateCheckIntervalTooLow", "### 更新检查间隔过低将造成性能问题! ###");
        m.timer = gm("timer", "更新检查将在 %1 秒后运行, 并以每 %2 秒的间隔重复运行");
        m.commandReloadOnUpdating = gm("commandReloadOnUpdating", "当前正在运行更新, 配置重载将被推迟");
        m.commandReloadOK = gm("commandReloadOK", "已完成重载");
        m.commandRepeatedRunUpdate = gm("commandRepeatedRunUpdate", "已有一个未完成的更新正在运行");
        m.commandUpdateStart = gm("commandUpdateStart", "更新开始运行!");
        m.commandFullLog = gm("commandFullLog", "完整日志:");
        m.commandStopUpdateIng = gm("commandStopUpdateIng", "正在停止当前更新...");
        m.stopUpdate = gm("stopUpdate", "已停止当前更新");
        m.repeatedRunUpdate = gm("repeatedRunUpdate", "### 更新程序重复启动或出现错误? 尝试提高更新检查间隔? ###");
        m.updateStart = gm("updateStart", "[## 开始运行自动更新 ##]");
        m.configErrList = gm("configErrList", "更新列表配置错误? ");
        m.configErrUpdate = gm("configErrUpdate", "更新列表配置错误? 项目为空");
        m.configErrMissing = gm("configErrMissing", "更新列表配置错误? 缺少基本配置");
        m.updateChecking = gm("updateChecking", "正在检查更新...");
        m.updateErrParsingDUrl = gm("updateErrParsingDUrl", "解析文件直链时出现错误, 将跳过此更新");
        m.updateTempAlreadyLatest = gm("updateTempAlreadyLatest", "[缓存] 文件已是最新版本");
        m.updateErrDownload = gm("updateErrDownload", "下载文件时出现异常, 将跳过此更新");
        m.updateZipFileCheck = gm("updateZipFileCheck", "[Zip 完整性检查] 文件不完整, 将跳过此更新");
        m.updateFileAlreadyLatest = gm("updateFileAlreadyLatest", "文件已是最新版本");
        m.updateFulSizeDifference = gm("updateFulSizeDifference", "更新完成 [%1MB] -> [%2MB]");
        m.updateFul = gm("updateFul", "[## 更新全部完成 ##]");
        m.updateFulTime = gm("updateFulTime", "耗时: %1 秒");
        m.updateFulFail = gm("updateFulFail", "失败: %1, ");
        m.updateFulUpdate = gm("updateFulUpdate", "更新: %1, ");
        m.updateFulOK = gm("updateFulOK", "完成: %1");
        m.updateFulNetRequest = gm("updateFulNetRequest", "网络请求: %1, ");
        m.updateFulDownloadFile = gm("updateFulDownloadFile", "下载文件: %1MB");
        m.logReloadOK = gm("logReloadOK", "已完成重载");
        m.debugGetVersion = gm("debugGetVersion", "找到版本: %1");
        m.debugNoFileMatching = gm("debugNoFileMatching", "没有匹配的文件: %1");
        m.debugNoRepositoryPath = gm("debugNoRepositoryPath", "未找到存储库路径: %1");
        m.debugErrUrlResolveNoID = gm("debugErrUrlResolveNoID", "URL 解析错误, 不包含插件 ID?: %1");
        m.debugErrUrlResolveNoName = gm("debugErrUrlResolveNoName", "URL 解析错误, 未找到项目名称: %1");
        m.debugErrNoID = gm("debugErrNoID", "未找到项目 ID: %1");
        m.httpRequestFailed = gm("httpRequestFailed", "请求失败? (%1): %2");
        m.urlInvalid = gm("urlInvalid", "URL 无效或不规范: %1");
    }
}
