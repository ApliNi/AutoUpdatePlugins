package io.github.aplini.autoupdateplugins.data.config;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;

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
    private Proxy proxy = new Proxy();
    private ArrayList<String> logLevel = new ArrayList<>() {{
        this.add("DEBUG");
        this.add("MARK");
        this.add("INFO");
        this.add("WARN");
        this.add("NET_WARN");
    }};
    private LinkedList<Header> setRequestProperty = new LinkedList<>(){{
        add(new Header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
    }};
    @Getter
    @Setter
    public static class Paths {
        private String updatePath = "./plugins/update/";
        private String tempPath = "./plugins/AutoUpdatePlugins/temp/";
        private String filePath = "./plugins/";
    }
    @Getter
    @Setter
    public static class Proxy {
        private java.net.Proxy.Type type = java.net.Proxy.Type.DIRECT;
        private String host = "127.0.0.1";
        private int port = 7890;
    }
    @Getter
    @Setter
    public static class Header {
        private String name;
        private String value;
        public Header() {}
        public Header(String k, String v) {
            name = k;
            value = v;
        }
    }
}
