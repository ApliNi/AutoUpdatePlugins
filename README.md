## AutoUpdatePlugins

[[中文文档]](https://github.com/ApliNi/AutoUpdatePlugins/blob/main/README.md) -- [[English document]](https://github.com/ApliNi/AutoUpdatePlugins/blob/main/README_EN.md)

更好的自动更新插件

下载: https://modrinth.com/plugin/AutoUpdatePlugins

---

## 功能和指令
- `/aup` 显示插件信息
    - `/aup reload` 重新加载配置
    - `/aup update` 手动运行更新
    - `/aup log` 查看完整日志
    - `/aup stop` 停止当前更新


- [x] 使用 `update` 目录进行插件更新
- [x] 根据插件发布页自动找到下载链接
  - `GitHub, Jenkins, Spigot, Modrinth, Bukkit, 鬼斩构建站 v2`
    - 支持下载 GitHub 中的预发布版本
- [x] 支持匹配相同发布下的不同文件
  - `GitHub, Jenkins, Modrinth`
- [x] 支持文件完整性检查
- [x] 缓存上一个更新的信息, 不重复下载文件
- [x] 不重复安装更新
- [x] 每个更新任务可以单独添加配置
- [x] 可配置的证书验证
- [x] 自定义输出日志等级
- [x] 多语言支持!
- [ ] 支持更新时运行系统命令


[[使用量统计]](https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629)
<a href="https://bstats.org/plugin/bukkit/ApliNi-AutoUpdatePlugins/20629">![](https://bstats.org/signatures/bukkit/ApliNi-AutoUpdatePlugins.svg)</a>

**运行日志**
```yaml
[INFO]: [AUP] 更新检查将在 64 秒后运行, 并以每 14400 秒的间隔重复运行
[INFO]: [AUP] [## 开始运行自动更新 ##]
[INFO]: [AUP] [EssentialsX.jar] 正在更新...
[INFO]: [AUP] [EssentialsX.jar] [Github] 找到版本: https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsX-2.20.1.jar
[INFO]: [AUP] [EssentialsX.jar] 更新完成 [1.17MB] -> [2.92MB]
[INFO]: [AUP] [EssentialsXChat.jar] 正在更新...
[INFO]: [AUP] [EssentialsXChat.jar] [Github] 找到版本: https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsXChat-2.20.1.jar
[INFO]: [AUP] [EssentialsXChat.jar] 更新完成 [0.01MB] -> [0.01MB]
[INFO]: [AUP] [CoreProtect.jar] 正在更新...
[INFO]: [AUP] [CoreProtect.jar] [Modrinth] 找到版本: https://cdn.modrinth.com/data/Lu3KuzdV/versions/w3P6ufP1/CoreProtect-22.2.jar
[INFO]: [AUP] [CoreProtect.jar] 文件已是最新版本
...
[INFO]: [AUP] [Dynmap网页地图.jar] 正在更新...
[WARN]: [AUP] [Dynmap网页地图.jar] [HTTP] 请求失败? (403): https://legacy.curseforge.com/minecraft/bukkit-plugins/dynmap
[WARN]: [AUP] [Dynmap网页地图.jar] [CurseForge] 解析文件直链时出现错误, 将跳过此更新
[INFO]: [AUP] [## 更新全部完成 ##]
[INFO]: [AUP]   - 耗时: 268 秒
[INFO]: [AUP]   - 失败: 2, 更新: 22, 完成: 24
[INFO]: [AUP]   - 网络请求: 48, 下载文件: 40.10MB
```


### 配置
```yaml

# 服务器启动完成后等待多长时间开始运行第一次更新 (秒
startupDelay: 64

# 第一次运行完成后以此频率重复运行更新 (秒, 修改后需要重启
startupCycle: 14400 # 4小时

# 插件更新目录, 设置与 bukkit.yml 中的一致
# 注意路径最后一定是 "/"
updatePath: './plugins/update/'

# 下载缓存目录, 无需修改
# 新的 .jar 文件会先下载到缓存目录, 验证完成后再移动到插件更新目录
tempPath: './plugins/AutoUpdatePlugins/temp/'

# 服务器正在运行的插件或文件的目录, 用于哈希检查
filePath: './plugins/'

# 启用上一个更新记录并通过这些信息检查更新 (temp.yml)
enablePreviousUpdate: true

# 文件完整性检查, 只对 .jar / .zip 等文件有效. 尝试以压缩包的形式打开, 若失败则表示不完整
zipFileCheck: true
# 如果 file 配置与此正则匹配, 则启用 zip 完整性检查, 否则不会启用
zipFileCheckList: '\.(?:jar|zip)$'

# 如果下载后的文件哈希与更新目录中待更新的文件 (或者服务器正在运行的文件) 哈希值一致则不移动到更新目录 (MD5
ignoreDuplicates: true

# 全局禁用证书验证, 修改后需要重启
disableCertificateVerification: false

# HTTP 请求中编辑请求头
setRequestProperty:
  - name: 'User-Agent'
    value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'

# 启用哪些日志等级
logLevel:
  - 'DEBUG'   # 用于调试, 可在测试完成后注释掉
  - 'MARK'    # 与 DEBUG 相同, 显示绿色, 用于标记任务完成
  - 'INFO'    # 输出日志
  - 'WARN'    # 输出警告
  - 'NET_WARN'  # 网络请求模块的警告


# 插件列表
# URL 支持自动下载 `GitHub, Jenkins, SpigotMC, Modrinth, Bukkit, 鬼斩构建站 v2` 页面的插件, 其他链接将直接下载
# 其中 `GitHub, Jenkins, Modrinth` 页面可以使用 get 参数下载指定文件
# GitHub 链接可添加配置 `getPreRelease: true` 来下载最新的预发布版本
list:

  - file: 'AutoUpdatePlugins自动更新.jar'
    url: https://github.com/ApliNi/AutoUpdatePlugins/

### 示例配置 ### 测试时注意 Yaml 格式

#  - file: 'EssentialsX.jar' # Github
#    url: https://github.com/EssentialsX/Essentials
#    get: 'EssentialsX-([0-9.]+)\.jar'  # 如果 GitHub/Jenkins 发布中存在多个文件, 则需要匹配其中一个, 否则下载第一个 (使用正则表达式

#  - file: 'EssentialsXChat.jar' # 匹配相同发布中的不同文件
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

#  - file: 'UseTranslatedNames翻译物品名.jar'
#    url: https://modrinth.com/plugin/usetranslatednames

#  - file: 'HttpRequests网络请求.jar' # SpigotMC
#    url: https://www.spigotmc.org/resources/http-requests.101253/

#  - file: 'SF4_Slimefun4粘液科技.jar' # 鬼斩构建站 v2
#    url: https://builds.guizhanss.com/StarWishsama/Slimefun4/master

#  - file: 'SF4_FluffyMachines蓬松科技.jar'
#    url: https://builds.guizhanss.com/SlimefunGuguProject/FluffyMachines/master

#  # 可以像这样为每个文件添加配置
#  # 如果 file 配置中包含路径, 则自动设置 path 参数
#  - file: './serverConfig.yml'
#    url: 'http://[::]:5212/serverConfig.yml'


### list 中的所有可用配置 ###
# 除非你知道这是在做什么, 否则不要随意随意使用
# String file;              // 文件名称
# String url;               // 下载链接
# String tempPath;          // 下载缓存路径, 默认使用全局配置
# String updatePath;        // 更新存放路径, 默认使用全局配置
# String filePath;          // 最终安装路径, 默认使用全局配置
# String path;              // 同时覆盖 updatePath 和 filePath 配置
# String get;               // 选择指定文件的正则表达式, 默认选择第一个. 仅限 GitHub, Jenkins, Modrinth
# boolean getPreRelease;    // 允许下载预发布版本, 默认 false. 仅限 GitHub
# boolean zipFileCheck;     // 启用 zip 文件完整性检查
# boolean ignoreDuplicates; // 关闭哈希检查

```

一些功能和代码参考了项目 [NewAmazingPVP/AutoUpdatePlugins](https://github.com/NewAmazingPVP/AutoUpdatePlugins).
