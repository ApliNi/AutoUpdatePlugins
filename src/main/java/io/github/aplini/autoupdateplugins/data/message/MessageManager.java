package io.github.aplini.autoupdateplugins.data.message;

import com.google.common.io.ByteStreams;
import io.github.aplini.autoupdateplugins.AutoUpdate;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MessageManager {
    @Getter
    private  MessageInstance instance;
    private final File file;
    public MessageManager(String lang, AutoUpdate plugin) throws IOException {
        file = new File(plugin.getDataFolder() + "message.yml");
        if(file.exists()) {
            file.createNewFile();
            InputStream is;
            try {
                is = plugin.getResource("messages/" + lang + ".yml");
            } catch (NullPointerException e) {
                is = plugin.getResource("messages/zh-CN.yml");
            }
            ByteStreams.copy(Objects.requireNonNull(is), new FileOutputStream(file));
        }
        FileConfiguration messageReader = YamlConfiguration.loadConfiguration(file);
        instance = messageReader.getObject("", MessageInstance.class,new MessageInstance());
    }
    public void reload(){
        FileConfiguration messageReader = YamlConfiguration.loadConfiguration(file);
        instance = messageReader.getObject("", MessageInstance.class,new MessageInstance());
    }
}
