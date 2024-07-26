package io.github.aplini.autoupdateplugins.data.config;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ConfigInstance {
    private String language = "zh-CN";
    private int startupDelay = 60;
    private int startupCycle = 14400;
    private int downloadThreadCount = 8;
    private Paths paths = new Paths();
    private boolean enablePreviousUpdate = true;
    private boolean zipFileCheck = true;
    private String zipFileCheckPattern = "\\.(?:jar|zip)$";
    private boolean ignoreDuplicates = true;
    private boolean sslVerify = true;
    private Proxy proxy = new Proxy(Proxy.Type.DIRECT, new InetSocketAddress("127.0.0.1", 7890));
    private ArrayList<String> logLevel = new ArrayList<>() {{
        this.add("DEBUG");
        this.add("MARK");
        this.add("INFO");
        this.add("WARN");
        this.add("NET_WARN");
    }};
    private Map<String, String> setRequestProperty = new HashMap<>() {{
        this.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
    }};
    @Getter
    @Setter
    public static class Paths {
        private String update = "./plugins/update/";
        private String temp = "./plugins/AutoUpdatePlugins/temp/";
        private String file = "./plugins/";
    }
}
