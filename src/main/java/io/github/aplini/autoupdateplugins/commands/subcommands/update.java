package io.github.aplini.autoupdateplugins.commands.subcommands;

import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import io.github.aplini.autoupdateplugins.commands.SubCommand;
import org.bukkit.command.CommandSender;


import java.util.List;

public class update extends SubCommand {
    private final AutoUpdatePlugin plugin;
    public update(AutoUpdatePlugin plugin) {
        this.name = "update";
        this.permission = "aup.admin";
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getUPDATE();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getUPDATE();
        this.plugin = plugin;
    }
    @Override
    public void onCommand(CommandSender sender, String[] args) {
        plugin.getUpdateInstance().run(sender);
    }

    @Override
    public List<String> onTab(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public void reloadMessage() {
        this.usage = plugin.getMessageManager().getInstance().getCommands().getUsage().getUPDATE();
        this.description = plugin.getMessageManager().getInstance().getCommands().getDescription().getUPDATE();
    }
}
