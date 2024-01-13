## AutoUpdatePlugins
Better auto update plugins

---

## Functions and commands
- `/aup` Show plugin information
    - `/aup reload` Reload configuration
    - `/aup update` Run updates manually


- [x] Use the `update` directory for plugin updates
- [x] Automatically find the download link based on the plug-in release page
    - `Github, Jenkins, Spigot, Modrinth, Bukkit, 鬼斩构建站 v2`
        - Support downloading pre-release versions from Github
- [x] Support matching different files under the same release
    - `Github, Jenkins, Modrinth`
- [x] Support file integrity check
- [x] Cache the last updated information and do not download files repeatedly
- [x] Do not install updates repeatedly
- [x] Configuration can be added individually for each update task
- [x] Configurable certificate verification


[[Usage statistics]](https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629)
<a href="https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629">![](https://bstats.org/signatures/bukkit/ApliNi-AutoUpdatePlugins.svg)</a>

Some functions and code refer to project [NewAmazingPVP/AutoUpdatePlugins](https://github.com/NewAmazingPVP/AutoUpdatePlugins).
