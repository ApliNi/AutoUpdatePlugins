package io.github.aplini.autoupdateplugins.commands.subcommands;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.commands.SubCommand;
import io.github.aplini.autoupdateplugins.utils.Util;
import org.bukkit.entity.Player;

import java.util.List;

public class update extends SubCommand {
    private final AutoUpdate plugin;
    public update(AutoUpdate plugin) {
        this.name = "update";
        this.permission = "aup.admin";
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getUPDATE();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getUPDATE();
        this.plugin = plugin;
    }
    @Override
    public void onCommand(Player player, String[] args) {
        plugin.getUpdateInstance().run(player);
    }

    @Override
    public List<String> onTab(Player player, String[] args) {
        return null;
    }

    @Override
    public void reloadMessage() {
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getUPDATE();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getUPDATE();
    }
}
