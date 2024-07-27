package io.github.aplini.autoupdateplugins.data.message;

import com.google.common.io.ByteStreams;
import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.Objects;

public class MessageManager {
    private final File file;
    private final Yaml yaml;
    @Getter
    private MessageInstance instance;

    public MessageManager(String lang, AutoUpdatePlugin plugin) throws IOException {
        file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            file.createNewFile();
            InputStream is;
            try {
                is = plugin.getResource("messages/" + lang + ".yml");
            } catch (NullPointerException e) {
                is = plugin.getResource("messages/zh-CN.yml");
            }
            ByteStreams.copy(Objects.requireNonNull(is), new FileOutputStream(file));
        }
        yaml = new Yaml(new Constructor(
                MessageInstance.class,
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
        try (FileReader reader = new FileReader(file)) {
            this.instance = yaml.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
