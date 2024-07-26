package io.github.aplini.autoupdateplugins.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
}
