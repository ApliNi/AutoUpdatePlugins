## AutoUpdatePlugins

[[中文文档]](https://github.com/ApliNi/AutoUpdatePlugins/blob/main/README.md) -- [[English document]](https://github.com/ApliNi/AutoUpdatePlugins/blob/main/README_EN.md)

Download: https://modrinth.com/plugin/AutoUpdatePlugins

---

## Functions and commands
- `/aup` Show plugin information
    - `/aup reload` Reload configuration
    - `/aup update` Run update manually
    - `/aup log` View full logs
    - `/aup stop` Stop the current update


- [x] Using the `update` directory for plugin updates
- [x] Automatically find download links based on plugin release page
    - `GitHub, Jenkins, Spigot, Modrinth, Bukkit, Ghost Chop Build Station v2`.
        - Support downloading pre-releases from GitHub.
- [x] Support matching different files under the same release
    - `GitHub, Jenkins, Modrinth`.
- [x] Support for file integrity checking
- [x] Cache last update, no duplicate downloads.
- [x] No duplicate installation of updates
- [x] Each update task can be configured individually.
- [x] Configurable certificate validation
- [x] Customizable output log level
- [x] Multi-language support!
- [ ] Support for running system commands while updating


[[Usage statistics]](https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629)
<a href="https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629">![](https://bstats.org/signatures/bukkit/ApliNi-AutoUpdatePlugins.svg)</a>

**运行日志**
```yaml
[INFO]: [AUP] The update check will run after 64 seconds and repeat every 14400 seconds.
[INFO]: [AUP] [## Start running automatic updates ##]
[INFO]: [AUP] [EssentialsX.jar] Updating...
[INFO]: [AUP] [EssentialsX.jar] [Github] Version found: https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsX-2.20.1. jar
[INFO]: [AUP] [EssentialsX.jar] Update complete [1.17MB] -> [2.92MB]
[INFO]: [AUP] [EssentialsXChat.jar] Updating...
[INFO]: [AUP] [EssentialsXChat.jar] [Github] Version found: https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsXChat- 2.20.1.jar
[INFO]: [AUP] [EssentialsXChat.jar] Update complete [0.01MB] -> [0.01MB]
[INFO]: [AUP] [CoreProtect.jar] Updating...
[INFO]: [AUP] [CoreProtect.jar] [Modrinth] Version found: https://cdn.modrinth.com/data/Lu3KuzdV/versions/w3P6ufP1/CoreProtect-22.2.jar
[INFO]: [AUP] [CoreProtect.jar] file is up to date!
...
[INFO]: [AUP] [Dynmap.jar] is being updated...
[WARN]: [AUP] [Dynmap.jar] [HTTP] Request failed? (403): https://legacy.curseforge.com/minecraft/bukkit-plugins/dynmap
[WARN]: [AUP] [Dynmap.jar] [CurseForge] Error parsing direct file link, will skip this update.
[INFO]: [AUP] [## Update all done ##]
[INFO]: [AUP] - Time consumed: 268 seconds
[INFO]: [AUP] - Failed: 2, Updated: 22, Completed: 24
[INFO]: [AUP] - Network requests: 48, Downloaded files: 40.10MB
```


### 配置
```yaml

# How long to wait for the first update to run after the server has finished booting (seconds)
startupDelay: 64

# After the first run is complete, repeat the update at this frequency (seconds, reboot required after modification).
startupCycle: 14400 # 4h

# Plugin update directory, set as in bukkit.yml
# Note that the path must end with "/"
updatePath: './plugins/update/'

# Download the cache directory without modification
# New .jar files will be downloaded to the cache directory first, then moved to the plugin update directory after validation is complete
tempPath: './plugins/AutoUpdatePlugins/temp/'

# Directory of plugins or files the server is running, for hash checking
filePath: './plugins/'

# Enable the last update record and check for updates with this information (temp.yml)
enablePreviousUpdate: true

# File integrity check, only works on .jar / .zip files. Try to open it as a zip file, if it fails, it is incomplete.
zipFileCheck: true

# Do not move to the update directory if the hash of the downloaded file matches the hash of the file to be updated in the update directory (or the file running on the server) (MD5)
ignoreDuplicates: true

# Disable certificate validation globally, need to reboot after modification.
disableCertificateVerification: false

# Edit request headers in HTTP requests
setRequestProperty:
  - name: 'User-Agent'
    value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

# 启用哪些日志等级
logLevel:
  - "DEBUG"   # for debugging, can be commented out after testing is complete
  - "MARK"    # Same as DEBUG, green color, used to mark task completion.
  - "INFO"    # Output logs
  - "WARN"    # Output warnings
  - "NET_WARN"  # Warnings from the web request module


# Plugin List
# The URL supports automatic download of plugins from `GitHub, Jenkins, SpigotMC, Modrinth, Bukkit, 鬼斩构建站 v2` pages, while other links will download them directly.
# One of the `GitHub, Jenkins, Modrinth` pages can use the get parameter to download a specific file.
# GitHub links can be configured with `getPreRelease: true` to download the latest pre-release version.
list:

  - file: 'AutoUpdatePlugins自动更新.jar'
    url: https://github.com/ApliNi/AutoUpdatePlugins/

### Example configurations ### Note Yaml formatting during testing

#  - file: 'EssentialsX.jar' # Github
#    url: https://github.com/EssentialsX/Essentials
#    get: 'EssentialsX-([0-9.]+)\.jar'  # If there is more than one file in the GitHub/Jenkins distribution, you need to match one of them, otherwise download the first one (using the regular expression

#  - file: 'EssentialsXChat.jar' # Match different files in the same release
#    url: https://github.com/EssentialsX/Essentials
#    get: 'EssentialsXChat-([0-9.]+)\.jar'

#  - file: 'Geyser-Spigot.jar' # URL
#    url: https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot

#  - file: 'ViaVersion-DEV.jar' # Jenkins
#    url: https://ci.viaversion.com/job/ViaVersion-DEV/

#  - file: 'ViaBackwards-DEV.jar'
#    url: https://ci.viaversion.com/view/ViaBackwards/job/ViaBackwards-DEV/

#  - file: 'CoreProtect.jar' # Modrinth
#    url: https://modrinth.com/plugin/coreprotect/

#  - file: 'UseTranslatedNames.jar'
#    url: https://modrinth.com/plugin/usetranslatednames

#  - file: 'HttpRequests.jar' # SpigotMC
#    url: https://www.spigotmc.org/resources/http-requests.101253/

#  - file: 'SF4_Slimefun4.jar' # 鬼斩构建站 v2
#    url: https://builds.guizhanss.com/StarWishsama/Slimefun4/master

#  - file: 'SF4_FluffyMachines.jar'
#    url: https://builds.guizhanss.com/SlimefunGuguProject/FluffyMachines/master

#  # Configuration can be added to each file like this
#  - file: 'serverConfig.yml'
#    url: 'http://[::]:5212/serverConfig.yml'
#    updatePath: './'     # Setting up a separate update directory
#    filePath: './'       # Setting the directory for hash checking
#    zipFileCheck: false  # Close integrity check


### All available configurations in list ###
# String file;              // File name
# String url;               // Download link
# String tempPath;          // download cache path, use global configuration by default
# String updatePath;        // update path, used globally by default
# String filePath;          // The final installation path, used globally by default.
# String get;               // Regular expression to select the specified file, first one is selected by default. Github, Jenkins, Modrinth only.
# boolean zipFileCheck;     // Enable zip file integrity checking, true by default.
# boolean getPreRelease;    // Allow downloading of pre-releases, false by default. GitHub only.

```

Some of the features and code references are from project [NewAmazingPVP/AutoUpdatePlugins](https://github.com/NewAmazingPVP/AutoUpdatePlugins).
