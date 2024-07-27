package io.github.aplini.autoupdateplugins.data.config;

import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private final AutoUpdatePlugin plugin;
    @Getter
    private ConfigInstance instance;
    private final File file;
    private final Yaml yaml;
    public ConfigManager(AutoUpdatePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        yaml = new Yaml(new Constructor(
                ConfigInstance.class,
                new LoaderOptions()
        ), new Representer(new DumperOptions()){{
            getPropertyUtils().setSkipMissingProperties(true);
        }}, new DumperOptions(){{
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

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(yaml.dumpAs(instance, Tag.MAP, DumperOptions.FlowStyle.BLOCK));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration reader = plugin.getConfig();
        instance = reader.getObject("", ConfigInstance.class, new ConfigInstance());
    }
}
