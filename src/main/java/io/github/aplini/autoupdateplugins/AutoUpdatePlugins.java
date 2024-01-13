package io.github.aplini.autoupdateplugins;

import com.google.gson.Gson;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;


public final class AutoUpdatePlugins extends JavaPlugin implements Listener, CommandExecutor, TabExecutor {
    boolean lock = false;
    boolean awaitReload = false;
    boolean debugLog = true;
    Timer timer = null;

    File tempFile;
    FileConfiguration temp;

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

        debugLog = getConfig().getBoolean("debugLog", true);

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
    @Override
    public void onDisable() {}


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
    }

    public void loadConfig(){
        reloadConfig();
        debugLog = getConfig().getBoolean("debugLog", true);

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
            getLogger().warning("### 更新检查间隔过低将造成性能问题! ###");
            startupCycle = 512;
        }
        // 计时器
        getLogger().info("更新检查将在 "+ startupDelay +" 秒后运行, 并以每 "+ startupCycle +" 秒的间隔重复运行");
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new updatePlugins(), startupDelay * 1000, startupCycle * 1000);
    }

    @Override // 运行指令
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // 默认输出插件信息
        if(args.length == 0){
            sender.sendMessage("IpacEL > AutoUpdatePlugins: 自动更新插件");
            sender.sendMessage("  指令: ");
            sender.sendMessage("    - /aup reload - 重载配置");
            sender.sendMessage("    - /aup update - 运行更新");
            return true;
        }

        // 重载配置
        else if(args[0].equals("reload")){
            if(lock){
                awaitReload = true;
                sender.sendMessage("[AUP] 当前正在运行更新, 配置重载将被推迟");
                return true;
            }
            loadConfig();
            sender.sendMessage("[AUP] 已完成重载");
            setTimer();
            return true;
        }

        // 调试模式
        else if(args[0].equals("update")){
            if(lock && !getConfig().getBoolean("disableLook", false)){
                sender.sendMessage("[AUP] 已有一个未完成的更新正在运行");
                return true;
            }
            sender.sendMessage("[AUP] 更新开始运行!");
            new Timer().schedule(new updatePlugins(), 0);
            return true;
        }
        return false;
    }

    @Override // 指令补全
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return  List.of(
                    "reload",   // 重载插件
                    "update"    // 运行更新
            );
        }
        return null;
    }

    private class updatePlugins extends TimerTask {
        String _nowFile = "[???] ";     // 当前文件的名称
        String _nowParser = "[???] ";   // 用于解析直链的解析器名称
        int _fail = 0;              // 更新失败的数量
        long _startTime;            // 最终耗时
        float _allFileSize;         // 已下载的文件大小合计

        // 在这里存放当前插件的配置
        String c_file;              // 文件名称
        String c_url;               // 下载链接
        String c_tempPath;          // 下载缓存路径, 默认使用全局配置
        String c_updatePath;        // 更新存放路径, 默认使用全局配置
        String c_filePath;          // 最终安装路径, 默认使用全局配置
        String c_get;               // 查找单个文件的正则表达式, 默认选择第一个. 仅限 Github, Jenkins, Modrinth
        boolean c_zipFileCheck;     // 启用 zip 文件完整性检查, 默认 true
        boolean c_getPreRelease;    // 允许下载预发布版本, 默认 false. 仅限 Github

        public void run() {
            // 防止重复运行
            if(lock && !getConfig().getBoolean("disableLook", false)){
                getLogger().warning("### 更新程序重复启动或出现错误? 尝试提高更新检查间隔? ###");
                return;
            }
            lock = true;
            _startTime = System.nanoTime();
            // 新线程
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                getLogger().info("[## 开始运行自动更新 ##]");

                List<?> list = (List<?>) getConfig().get("list");
                if(list == null){
                    getLogger().warning("更新列表配置错误? ");
                    return;
                }

                for(Object _li : list){
                    _fail ++;

                    Map<?, ?> li = (Map<?, ?>) _li;
                    if(li == null){
                        getLogger().warning("更新列表配置错误? 项目为空");
                        continue;
                    }

                    // 检查基础配置
                    c_file = (String) SEL(li.get("file"), "");
                    c_url = ((String) SEL(li.get("url"), "")).trim();
                    if(c_file.isEmpty() || c_url.isEmpty()){
                        getLogger().warning("更新列表配置错误? 缺少基本配置");
                        continue;
                    }

                    _nowFile = "["+ c_file +"] "; // 用于显示日志的插件名称
                    c_tempPath = getPath(getConfig().getString("tempPath", "./plugins/AutoUpdatePlugins/temp/")) + c_file;

                    // 每个单独的配置
                    c_updatePath = getPath((String) SEL(li.get("updatePath"), getConfig().getString("updatePath", "./plugins/update/"))) + c_file;
                    c_filePath = getPath((String) SEL(li.get("filePath"), getConfig().getString("filePath", "./plugins/"))) + c_file;
                    c_get = (String) SEL(li.get("get"), "");
                    c_zipFileCheck = (boolean) SEL(li.get("zipFileCheck"), true);
                    c_getPreRelease = (boolean) SEL(li.get("getPreRelease"), false);

                    // 下载文件到缓存目录
                    outInfo("正在检查更新...");
                    String dUrl = getFileUrl(c_url, c_get);
                    if(dUrl == null){
                        getLogger().warning(_nowFile + _nowParser +"解析文件直链时出现错误, 将跳过此更新");
                        continue;
                    }
                    dUrl = checkURL(dUrl);
//                    outInfo(dUrl);

                    // 启用上一个更新记录与检查
                    int contentLength = -1;
                    String pPath = "";
                    if(getConfig().getBoolean("enablePreviousUpdate", true)){
                        // 获取文件大小
                        contentLength = getContentLength(dUrl);
                        // 是否与上一个版本相同
                        pPath = "previous." + li.toString().hashCode();
                        if (temp.get(pPath) != null) {
                            // 检查数据差异
                            int i = 0;
                            if (!temp.getString(pPath + ".dUrl", "").equals(dUrl)) {i++;}
                            if (temp.getInt(pPath + ".contentLength", -1) != contentLength) {i++;}
                            if (i == 0) {
                                outInfo("[缓存] 文件已是最新版本");
                                _fail--;
                                continue;
                            }
                        }
                    }

                    if(!downloadFile(dUrl, c_tempPath)){
                        getLogger().warning(_nowFile +"下载文件时出现异常, 将跳过此更新");
                        new File(c_tempPath).delete();
                        continue;
                    }

                    // 记录文件大小
                    float fileSize = new File(c_tempPath).length();
                    _allFileSize += fileSize;

                    // 文件完整性检查
                    if(c_zipFileCheck && !isJARFileIntact(c_tempPath)){
                        getLogger().warning(_nowFile +"[Zip 完整性检查] 文件不完整, 下载链接可能已更新, 将跳过此更新");
                        new File(c_tempPath).delete();
                        continue;
                    }

                    // 此时已确保文件(信息)正常
                    if(getConfig().getBoolean("enablePreviousUpdate", true)){
                        // 更新数据
                        temp.set(pPath + ".file", c_file);
                        temp.set(pPath + ".time", nowDate());
                        temp.set(pPath + ".dUrl", dUrl);
                        temp.set(pPath + ".contentLength", contentLength);
                    }

                    // 在这里实现运行系统命令的功能

                    // 哈希值检查, 如果新文件哈希与更新目录中的相等, 或者与正在运行的版本相等, 则无需更新
                    String tempFileHas = fileHash(c_tempPath);
                    String updatePathFileHas = fileHash(c_updatePath);
                    if(Objects.equals(tempFileHas, updatePathFileHas) || Objects.equals(tempFileHas, fileHash(c_filePath))){
                        outInfo("文件已是最新版本");
                        _fail --;
                        new File(c_tempPath).delete();
                        continue;
                    }

                    // 获取旧版本的文件大小
                    float oldFileSize = updatePathFileHas.equals("null") ? new File(c_filePath).length() : new File(c_updatePath).length();

                    // 移动到更新目录
                    try {
                        Files.move(Path.of(c_tempPath), Path.of(c_updatePath), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        getLogger().warning(e.getMessage());
                    }

                    // 更新完成, 并显示文件大小变化
                    outInfo("更新完成 ["+ String.format("%.3f", oldFileSize / 1048576) +"MB] -> ["+ String.format("%.3f", fileSize / 1048576) +"MB]");

                    _nowFile = "[???] ";
                    _nowParser = "[???] ";
                    _fail --;
                }

                saveDate();

                getLogger().info("[## 更新全部完成 ##]");
                getLogger().info("  - 耗时: "+ Math.round((System.nanoTime() - _startTime) / 1_000_000_000.0) +" 秒");
                if(_fail != 0){getLogger().warning("  - 失败: "+ _fail +" ("+ list.size() +")");}
                getLogger().info("  - 下载文件: "+ String.format("%.2f", _allFileSize / 1048576) +"MB");

                lock = false;

                // 运行被推迟的配置重载
                if(awaitReload){
                    awaitReload = false;
                    loadConfig();
                    getLogger().info("[AUP] 已完成重载");
                    setTimer();
                }
            });
            executor.shutdown();
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
//                getLogger().warning(e.getMessage()); // 文件不存在时会输出异常
            }
            return "null";
        }

        // 获取部分文件直链
        public  String getFileUrl(String _url, String matchFileName) {
            // 移除 URL 最后的斜杠
            String url = _url.replaceAll("/$", "");

            if(url.contains("://github.com/")){ // Github 发布
                _nowParser = "[Github] ";
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
                            outInfo(_nowParser +"找到版本: "+ dUrl);
                            return dUrl;
                        }
                    }
                    getLogger().warning(_nowFile +"[Github] 没有匹配的文件: "+ url);
                    return null;
                }
                getLogger().warning(_nowFile +"[Github] 未找到存储库路径: "+ url);
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
                        outInfo(_nowParser +"找到版本: "+ dUrl);
                        return dUrl;
                    }
                }
                getLogger().warning(_nowFile +"[Jenkins] 没有匹配的文件: "+ url);
                return null;
            }

            else if(url.contains("://www.spigotmc.org/")){ // Spigot 页面
                _nowParser = "[Spigot] ";
                // 获取插件 ID
                Matcher matcher = Pattern.compile("([0-9]+)$").matcher(url);
                if(matcher.find()){
                    String dUrl = "https://api.spiget.org/v2/resources/"+ matcher.group(1) +"/download";
                    outInfo(_nowParser +"找到版本: "+ dUrl);
                    return dUrl;
                }
                getLogger().warning(_nowFile +"[Spigot] URL 解析错误, 不包含插件 ID?: "+ url);
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
                            outInfo(_nowParser +"找到版本: "+ dUrl);
                            return dUrl;
                        }
                    }
                    getLogger().warning(_nowFile +"[Modrinth] 没有匹配的文件: "+ url);
                    return null;
                }
                getLogger().warning(_nowFile +"[Modrinth] URL 解析错误, 未找到项目名称: "+ url);
                return null;
            }

            else if(url.contains("://dev.bukkit.org/")){ // Bukkit 页面
                _nowParser = "[Bukkit] ";
                String dUrl = url +"/files/latest";
                outInfo(_nowParser +"找到版本: "+ dUrl);
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
                    outInfo(_nowParser +"找到版本: "+ dUrl);
                    return dUrl;
                }
                getLogger().warning(_nowFile + _nowParser +"未找到存储库路径: "+ url);
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
                    outInfo(_nowParser +"找到版本: "+ dUrl);
                    return dUrl;
                }
                getLogger().warning(_nowFile + _nowParser +"未找到项目 ID: "+ url);
                return null;
            }

            else{ // 没有匹配的项
                _nowParser = "[URL] ";
                outInfo(_nowParser + _url);
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
                getLogger().warning(_nowFile +"[HTTP] "+ e.getMessage());
            }
            cxn.disconnect();
            return null;
        }

        // 下载文件到指定位置, 并使用指定文件名
        public boolean downloadFile(String url, String path){
            new File(path).delete(); // 删除可能存在的旧文件
            HttpURLConnection cxn = getHttpCxn(url);
            if(cxn == null){return false;}
            try {
                BufferedInputStream in = new BufferedInputStream(cxn.getInputStream());
                Path savePath = Path.of(path);
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                cxn.disconnect();
                return true;
            } catch (Exception e) {
                getLogger().warning(_nowFile +"[HTTP] "+ e.getMessage());
            }
            cxn.disconnect();
            return false;
        }

        // 通过 HEAD 请求获取 Content-Length 字段
        public int getContentLength(String url){
            int cl = -1;
            try {
                HttpURLConnection cxn = (HttpURLConnection) new URI(url).toURL().openConnection();
                cxn.setRequestMethod("HEAD");
                cl = cxn.getContentLength();
                cxn.disconnect();
            } catch (Exception e) {
                getLogger().warning(_nowFile +"[HTTP.HEAD] "+ e.getMessage());
            }
            return cl;
        }

        // 获取 HTTP 连接
        public HttpURLConnection getHttpCxn(String url){
            HttpURLConnection cxn = null;
            try {
                cxn = (HttpURLConnection) new URI(url).toURL().openConnection();
                cxn.setRequestMethod("GET");
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
                getLogger().warning(_nowFile +"[HTTP] 请求失败? ("+ cxn.getResponseCode() +"): "+ url);
            } catch (Exception e) {
                getLogger().warning(_nowFile +"[HTTP] "+ e.getMessage());
            }
            if(cxn != null){cxn.disconnect();}
            return null;
        }

        // 在插件更新过程中输出尽可能详细的日志
        public void outInfo(String t) {
            if(debugLog){
                getLogger().info(_nowFile + t);
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
                getLogger().warning(_nowFile +"[URI] URL 无效或不规范: "+ url);
                return null;
            }
        }
    }
}
