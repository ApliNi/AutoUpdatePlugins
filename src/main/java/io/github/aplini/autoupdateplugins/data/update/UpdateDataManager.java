package io.github.aplini.autoupdateplugins.data.update;

import com.google.common.io.ByteStreams;
import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import io.github.aplini.autoupdateplugins.beans.UpdateItem;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.LinkedList;
import java.util.Objects;

public class UpdateDataManager {
    private final File file;
    private final Yaml yaml;
    @Getter
    private Instance instance;
    public UpdateDataManager(String lang, AutoUpdatePlugin plugin) throws IOException {
        file = new File(plugin.getDataFolder(), "update.yml");
        if (!file.exists()) {
            file.createNewFile();
            InputStream is;
            try {
                is = plugin.getResource("update/" + lang + ".yml");
            } catch (NullPointerException e) {
                is = plugin.getResource("update/zh-CN.yml");
            }
            ByteStreams.copy(Objects.requireNonNull(is), new FileOutputStream(file));
        }
        yaml = new Yaml(new Constructor(
                Instance.class,
                new LoaderOptions()
        ), new Representer(new DumperOptions()) {{
            getPropertyUtils().setSkipMissingProperties(true);
        }}, new DumperOptions() {{
            setIndent(2);
            setPrettyFlow(true);
            setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            isAllowUnicode();
        }});
        try (FileReader reader = new FileReader(file)) {
            this.instance = yaml.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        FileConfiguration messageReader = YamlConfiguration.loadConfiguration(file);
        instance = messageReader.getObject("", Instance.class, new Instance());
    }
    @Getter
    @Setter
    public static class Instance {
        LinkedList<UpdateItem> list = new LinkedList<>();
    }
}
