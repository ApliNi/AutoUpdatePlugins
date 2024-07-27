package io.github.aplini.autoupdateplugins.commands.subcommands;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.commands.SubCommand;
import io.github.aplini.autoupdateplugins.data.config.ConfigInstance;
import io.github.aplini.autoupdateplugins.utils.Util;
import org.bukkit.command.CommandSender;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import static io.github.aplini.autoupdateplugins.utils.Util.getUpdateInstance;

public class reload extends SubCommand {
    private final AutoUpdate plugin;

    public reload(AutoUpdate plugin) {
        this.name = "reload";
        this.permission = "aup.admin";
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getRELOAD();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getRELOAD();
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        Util.Message(sender, plugin.getMessageManager().getInstance().getReloadMessage());
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        plugin.getCommandManager().reload();
        plugin.getUpdateDataManager().reload();
        plugin.getTempDataManager().save();
        if(plugin.getUpdateInstance().stop()) {
            Proxy proxy = Proxy.NO_PROXY;
            ConfigInstance.Proxy p = plugin.getConfigManager().getInstance().getProxy();
            if(p.getType() != Proxy.Type.DIRECT) {
                proxy = new Proxy(p.getType(), new InetSocketAddress(p.getHost(), p.getPort()));
            }
            plugin.setUpdateInstance(getUpdateInstance(
                    plugin.getConfigManager().getInstance().getStartupDelay(),
                    plugin.getConfigManager().getInstance().getStartupCycle(),
                    proxy,
                    plugin.getConfigManager().getInstance().getSetRequestProperty(),
                    plugin,
                    plugin.getUpdateDataManager().getInstance().getList(),
                    plugin.getConfigManager().getInstance().getDownloadThreadCount(),
                    plugin.getConfigManager().getInstance().isSslVerify()));
        }
        Util.Message(sender, plugin.getMessageManager().getInstance().getSuccessMessage());
    }

    @Override
    public List<String> onTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void reloadMessage() {
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getRELOAD();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getRELOAD();
    }
}
