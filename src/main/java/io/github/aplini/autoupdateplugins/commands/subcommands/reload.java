package io.github.aplini.autoupdateplugins.commands.subcommands;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.commands.SubCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class reload extends SubCommand {
    AutoUpdate p;

    public reload(AutoUpdate plugin) {
        this.name = "reload";
        this.permission = "aup.admin";
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getRELOAD();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getRELOAD();
        p = plugin;
    }

    @Override
    public void onCommand(Player player, String[] args) {
        p.reloadConfig();
    }

    @Override
    public List<String> onTab(Player player, String[] args) {
        return null;
    }

    @Override
    public void reloadMessage() {
        this.usage = p.getMessageManager().getInstance().getCommands().getUsage().getRELOAD();
        this.description = p.getMessageManager().getInstance().getCommands().getDescription().getRELOAD();
    }
}
