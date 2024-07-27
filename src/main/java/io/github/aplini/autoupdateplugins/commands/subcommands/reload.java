package io.github.aplini.autoupdateplugins.commands.subcommands;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.commands.SubCommand;
import io.github.aplini.autoupdateplugins.utils.Util;
import org.bukkit.entity.Player;

import java.util.List;

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
    public void onCommand(Player player, String[] args) {
        Util.Message(player, plugin.getMessageManager().getInstance().getReloadMessage());
        if(plugin.getUpdateInstance().stop()) {
            //todo player.sendMessage(plugin.getMessageManager().getInstance().);
        }
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        plugin.getCommandManager().reload();
        plugin.getUpdateDataManager().reload();
        plugin.getTempDataManager().save();
        Util.Message(player, plugin.getMessageManager().getInstance().getSuccessMessage());
    }

    @Override
    public List<String> onTab(Player player, String[] args) {
        return null;
    }

    @Override
    public void reloadMessage() {
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getRELOAD();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getRELOAD();
    }
}
