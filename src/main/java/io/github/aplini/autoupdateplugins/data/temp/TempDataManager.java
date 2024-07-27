package io.github.aplini.autoupdateplugins.data.temp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import io.github.aplini.autoupdateplugins.beans.TempData;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class TempDataManager {
    @Getter
    private final Map<String, TempData> tempDataMap;
    private final File file;
    private final AutoUpdatePlugin plugin;
    public TempDataManager(AutoUpdatePlugin plugin) {
        this.plugin = plugin;
        Map<String, TempData> map;
        this.file = new File(plugin.getDataFolder(), "temp.json");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileReader reader = new FileReader(file)) {
                map = new Gson().fromJson(reader, new TypeToken<Map<String, TempData>>() {
                }.getType());
                if(map == null)
                    map = new HashMap<>();
            }
        } catch (JsonIOException | JsonSyntaxException e) {
            map = new HashMap<>();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,e.getMessage(),e);
            map = new HashMap<>();
        }
        this.tempDataMap = map;
    }
    public void save(){
        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(tempDataMap, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,e.getMessage(),e);
        }
    }
}
