[postbg]bg5.png[/postbg][markdown]

https://github.com/ApliNi/AutoUpdatePlugins

---

## AutoUpdatePlugins `v1.0`
更好的自动更新插件

[//]: # (下载: https://modrinth.com/plugin/AutoUpdatePlugins)

---

## 功能和指令
- `/aup` 显示插件信息
    - `/aup reload` 重新加载配置
    - `/aup update` 手动运行更新


- [x] 使用 `update` 目录进行插件更新
- [x] 根据插件发布页自动找到下载链接
  - `Github, Jenkins, Spigot, Modrinth, Bukkit`
- [x] 支持匹配相同发布下的不同文件
  - `Github, Jenkins, Modrinth`
- [x] 支持文件完整性检查
- [x] 不重复更新
- [x] 每个插件的单独配置
- [x] 可配置的证书验证
- [ ] 支持更新时运行系统命令


### 配置
```yaml

# 服务器启动完成后等待多长时间开始运行第一次更新 (秒, 修改后需要重启
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

# 文件完整性检查, 只对 .jar / .zip 等文件有效. 尝试以压缩包的形式打开, 若失败则表示不完整
zipFileCheck: true

# 如果下载后的文件哈希与更新目录中待更新的文件 (或者服务器正在运行的文件) 哈希值一致则不移动到更新目录 (MD5
ignoreDuplicates: true

# 全局禁用证书验证, 修改后需要重启
disableCertificateVerification: false

# 隐藏配置, 无需修改
#disableLook: false
#disableUpdateCheckIntervalTooLow: false
#bStats: true

# 插件列表
# URL 支持自动下载 `Github, Jenkins, Spigot, Modrinth, Bukkit` 页面的插件, 其他链接将直接下载
# 其中 `Github, Jenkins, Modrinth` 页面可以使用 get 参数下载指定文件
list:

### 示例配置 ### 测试时注意 Yaml 格式

#  - file: 'EssentialsX.jar'
#    url: https://github.com/EssentialsX/Essentials
#    get: 'EssentialsX-([0-9.]+)\.jar'  # 如果 Github/Jenkins 发布中存在多个文件, 则需要匹配其中一个, 否则下载第一个 (使用正则表达式

#  - file: 'EssentialsXChat.jar'
#    url: https://github.com/EssentialsX/Essentials
#    get: 'EssentialsXChat-([0-9.]+)\.jar'

#  - file: 'Geyser-Spigot.jar'
#    url: https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot

#  - file: 'ViaVersion-DEV.jar'
#    url: https://ci.viaversion.com/job/ViaVersion-DEV/

#  - file: 'ViaBackwards-DEV.jar'
#    url: https://ci.viaversion.com/view/ViaBackwards/job/ViaBackwards-DEV/

#  - file: 'CoreProtect.jar'
#    url: https://modrinth.com/plugin/coreprotect/

#  - file: 'UseTranslatedNames翻译物品名.jar'  # 可以修改最后安装的插件名称
#    url: https://modrinth.com/plugin/usetranslatednames

#  - file: 'HttpRequests网络请求.jar'
#    url: https://www.spigotmc.org/resources/http-requests.101253/

#  # 可以像这样为每个文件添加部分配置
#  - file: 'serverConfig.yml'
#    url: 'http://[::]:5212/serverConfig.yml'
#    updatePath: './'     # 设置单独的更新目录
#    filePath: './'       # 设置哈希检查的目录
#    zipFileCheck: false  # 关闭完整性检查

```

一些功能和代码参考了项目 [NewAmazingPVP/AutoUpdatePlugins](https://github.com/NewAmazingPVP/AutoUpdatePlugins).


---

### MCBBS
本插件所用所有代码均为原创,不存在借用/抄袭等行为
本插件为非盈利性插件，免费发布，严禁销售和转卖

[/markdown]
