package io.github.aplini.autoupdateplugins.utils;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.beans.UpdateItem;
import io.github.aplini.autoupdateplugins.data.config.ConfigInstance;
import io.github.aplini.autoupdateplugins.update.UpdateInstance;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

public class Util {
    public static void Message(CommandSender player, String s) {
        if (player == null) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', s));
    }
    public static String getPath(String path) {
        Path directory = Paths.get(path);
        try {
            Files.createDirectories(directory);
            return directory + "/";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getNonNullString(String str1, String str2, String defaultValue) {
        return (str1 != null) ? str1 : (str2 != null) ? str2 : defaultValue;
    }

    // 辅助方法：计算文件的 SHA-1 值
    public static String calculateSHA1(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                sha1Digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] sha1Hash = sha1Digest.digest();
        return bytesToHex(sha1Hash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public static boolean isJARFileIntact(String filePath) {
        // 是否启用完整性检查
        try {
            JarFile jarFile = new JarFile(new File(filePath));
            jarFile.close();
            return true;
        } catch (ZipException e) { // 文件不完整
            return false;
        } catch (Exception e) { // 其他异常
            return false;
        }
    }
    public static UpdateInstance getUpdateInstance(
            int delay, int cycle, Proxy proxy, List<ConfigInstance.Header> headers,
            AutoUpdate plugin, List<UpdateItem> items, int poolSize,
            boolean isCheckSSL) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxy(proxy).addInterceptor(new Interceptor() {
                    @NotNull
                    @Override
                    public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                        Headers.Builder _headers = new Headers.Builder();
                        for (ConfigInstance.Header header : headers)
                            _headers.add(header.getName(),header.getValue());
                        return chain.proceed(
                                chain.request().newBuilder()
                                        .headers(_headers.build())
                                        .build()
                        );
                    }
                });
        if(!isCheckSSL) {
            X509TrustManager trustAllCerts = new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            };
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());
                builder.sslSocketFactory(
                        context.getSocketFactory(),
                        trustAllCerts
                );
                builder.hostnameVerifier((hostname, session) -> true);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return new UpdateInstance(
                delay,
                cycle,
                builder.build(),
                items,
                plugin,
                poolSize
        );
    }
}
