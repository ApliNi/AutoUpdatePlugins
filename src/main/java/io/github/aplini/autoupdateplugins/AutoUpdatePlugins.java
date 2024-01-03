package io.github.aplini.autoupdateplugins;

import com.google.gson.Gson;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;


public final class AutoUpdatePlugins extends JavaPlugin implements Listener {
    public boolean lock = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
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
            SSLContext sslContext = null;
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


    @EventHandler // 服务器启动完成事件
    public void onServerLoad(ServerLoadEvent event) {
        // 异步
        CompletableFuture.runAsync(() -> {
            long startupDelay = getConfig().getLong("startupDelay", 64);
            long startupCycle = getConfig().getLong("startupCycle", 1440);
            // 检查更新间隔是否过低
            if(startupCycle < 256 && !getConfig().getBoolean("disableUpdateCheckIntervalTooLow", false)){
                getLogger().warning("### 更新检查间隔过低将造成性能问题! ###");
                startupCycle = 512;
            }
            // 计时器
            Timer timer = new Timer();
            timer.schedule(new updatePlugins(), startupDelay * 1000, startupCycle * 1000);
            getLogger().info("更新检查将在 "+ startupDelay +" 秒后运行, 并以每 "+ startupCycle +" 秒的间隔重复运行");
        });
    }

    private class updatePlugins extends TimerTask {
        String _nowFile = "[???] ";
        long _startTime;

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
//                getLogger().info("开始运行");

                List<Map<?, ?>> list = (List<Map<?, ?>>) getConfig().get("list");
                if(list == null){
                    getLogger().warning("更新列表配置错误? ");
                    return;
                }

                for(Map<?, ?> li : list){
                    if(li == null){
                        getLogger().warning("更新列表配置错误? 项目为空");
                        continue;
                    }

                    // 检查基础配置
                    String fileName = (String) SEL(li.get("file"), "");
                    String url = (String) SEL(li.get("url"), "");
                    if(fileName.isEmpty() || url.isEmpty()){
                        getLogger().warning("更新列表配置错误? 缺少基本配置");
                        continue;
                    }
                    _nowFile = "["+ fileName +"] ";
                    String tempPath = getPath(getConfig().getString("tempPath", "./plugins/AutoUpdatePlugins/temp/")) + fileName;

                    // 每个单独的配置
                    String filePath = getPath((String) SEL(li.get("filePath"), getConfig().getString("filePath", "./plugins/"))) + fileName;
                    String updatePath = getPath((String) SEL(li.get("updatePath"), getConfig().getString("updatePath", "./plugins/update/"))) + fileName;
                    String getMatchFileName = (String) SEL(li.get("get"), "");
                    boolean pluginFileCheck = (boolean) SEL(li.get("zipFileCheck"), true);

                    // 下载文件到缓存目录
                    getLogger().info(_nowFile +"正在更新...");
                    if(!downloadFile(getFileUrl(url, getMatchFileName), tempPath)){
                        getLogger().warning(_nowFile +"下载文件时出现异常");
                        new File(tempPath).delete();
                        continue;
                    }

                    // 文件完整性检查
                    if(!isJARFileIntact(tempPath, pluginFileCheck)){
                        getLogger().warning(_nowFile +"[完整性检查] 文件可能不完整: "+ url);
                        new File(tempPath).delete();
                        continue;
                    }

                    // 在这里实现运行系统命令的功能

                    // 哈希值检查, 如果新文件哈希与更新目录中的相等, 或者与正在运行的版本相等, 则无需更新
                    String tempFileHas = fileHash(tempPath);
                    if(Objects.equals(tempFileHas, fileHash(updatePath)) || Objects.equals(tempFileHas, fileHash(filePath))){
                        getLogger().info(_nowFile +"文件已是最新版本");
                        new File(tempPath).delete();
                        continue;
                    }

                    // 移动到更新目录
                    try {
                        Files.move(Path.of(tempPath), Path.of(updatePath), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        getLogger().warning(e.getMessage());
                    }

//                    getLogger().info(_nowFile +"更新完成");
                    _nowFile = "[???] ";
                }
                getLogger().info("更新全部完成, 耗时 "+ Math.round((System.nanoTime() - _startTime) / 1_000_000_000.0) +" 秒");
                lock = false;
            });
            executor.shutdown();
        }

        // 下载文件到指定位置, 并使用指定文件名
        public boolean downloadFile(String url, String path) {
            new File(path).delete(); // 删除可能存在的旧文件
            try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream())) {
                Path savePath = Path.of(path);
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                getLogger().warning(e.getMessage());
                return false;
            }
        }

        // 尝试打开 jar 文件以判断文件是否完整
        public boolean isJARFileIntact(String filePath, boolean Enable) {
            // 是否启用完整性检查
            if(getConfig().getBoolean("zipFileCheck", true) && Enable){
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
                // 获取路径 "/ApliNi/Chat2QQ"
                Matcher matcher = Pattern.compile("/([^/]+)/([^/]+)$").matcher(url);
                if(matcher.find()){
                    String data = httpGet("https://api.github.com/repos" + matcher.group(0) + "/releases/latest");
                    Map<?, ?> map = new Gson().fromJson(data, HashMap.class); // 解析 JSON
                    // 遍历发布文件列表
                    ArrayList<Map<?, ?>> assets = (ArrayList<Map<?, ?>>) map.get("assets");
                    for(Map<?, ?> li : assets){
                        String fileName = (String) li.get("name");
                        if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                            String dUrl = (String) li.get("browser_download_url");
                            getLogger().info(_nowFile +"[Github] 找到版本: "+ dUrl);
                            return dUrl;
                        }
                    }
                }
                getLogger().warning(_nowFile +"[Github] 未找到存储库路径: "+ url);
            }

            else if(url.contains("://ci.")){ // Jenkins
                // https://ci.viaversion.com/view/ViaBackwards/job/ViaBackwards-DEV/lastSuccessfulBuild/artifact/build/libs/ViaBackwards-4.10.0-23w51b-SNAPSHOT.jar
                String data = httpGet(url +"/lastSuccessfulBuild/api/json");
                Map<?, ?> map = new Gson().fromJson(data, HashMap.class);
                ArrayList<Map<?, ?>> artifacts = (ArrayList<Map<?, ?>>) map.get("artifacts");
                // 遍历发布文件列表
                for(Map<?, ?> li : artifacts){
                    String fileName = (String) li.get("fileName");
                    if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                        String dUrl = url +"/lastSuccessfulBuild/artifact/"+ li.get("relativePath");
                        getLogger().info(_nowFile +"[Jenkins] 找到版本: "+ dUrl);
                        return dUrl;
                    }
                }
            }

            else if(url.contains("://www.spigotmc.org/")){ // Spigot 页面
                // 获取插件 ID
                Matcher matcher = Pattern.compile("\\.([0-9]+)$").matcher(url);
                if(matcher.find()){
                    String dUrl = "https://api.spiget.org/v2/resources/"+ matcher.group(1) +"/download";
                    getLogger().info(_nowFile +"[Spigot] 找到版本: "+ dUrl);
                    return dUrl;
                }
                getLogger().warning(_nowFile +"[Spigot] 无法从URL中提取直链: "+ url);
            }

            else if(url.contains("://modrinth.com/")){ // Modrinth 页面
                Matcher matcher = Pattern.compile("/([^/]+)$").matcher(url);
                if(matcher.find()) {
                    String data = httpGet("https://api.modrinth.com/v2/project"+ matcher.group(0) +"/version");
                    // 0 为最新的一个版本
                    Map<?, ?> map = ((ArrayList<Map<?, ?>>) new Gson().fromJson(data, ArrayList.class)).get(0);
                    ArrayList<Map<?, ?>> files = (ArrayList<Map<?, ?>>) map.get("files");
                    // 遍历发布文件列表
                    for(Map<?, ?> li : files){
                        String fileName = (String) li.get("filename");
                        if(matchFileName.isEmpty() || Pattern.compile(matchFileName).matcher(fileName).matches()){
                            String dUrl = (String) li.get("url");
                            getLogger().info(_nowFile +"[Modrinth] 找到版本: "+ dUrl);
                            return dUrl;
                        }
                    }
                }
            }

            else if(url.contains("://dev.bukkit.org/")){ // Bukkit 页面
                String dUrl = url +"/files/latest";
                getLogger().info(_nowFile +"[Bukkit] 找到版本: "+ dUrl);
                return dUrl;
            }

            // 没有匹配的项
            getLogger().info(_nowFile +"[URL] "+ _url);
            return _url;
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
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                if(connection.getResponseCode() == 200){
                    // 读取响应内容
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    char[] buffer = new char[1024];
                    int len;
                    while ((len = reader.read(buffer)) > 0) {
                        response.append(buffer, 0, len);
                    }
                    reader.close();

                    return response.toString();
                }
                getLogger().warning("[HTTP] 请求失败? ("+ connection.getResponseCode() +"): "+ url);
            } catch (Exception e) {
                getLogger().warning("[HTTP] "+ e.getMessage());
            }
            return null;
        }
    }
}
