package io.github.aplini.autoupdateplugins.commands;

import io.github.aplini.autoupdateplugins.AutoUpdatePlugin;
import io.github.aplini.autoupdateplugins.commands.subcommands.reload;
import io.github.aplini.autoupdateplugins.commands.subcommands.stop;
import io.github.aplini.autoupdateplugins.commands.subcommands.update;
import io.github.aplini.autoupdateplugins.utils.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
    public final ArrayList<SubCommand> subCommands = new ArrayList<>();
    private final AutoUpdatePlugin plugin;

    public CommandManager(AutoUpdatePlugin plugin) {
        this.plugin = plugin;
        subCommands.add(new reload(plugin));
        subCommands.add(new update(plugin));
        subCommands.add(new stop(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            for (SubCommand subCommand : subCommands)
                Util.Message(sender, String.format("%s - %s", subCommand.usage, subCommand.description));
            return true;
        }
        for (SubCommand subCommand : subCommands)
            if (subCommand.name.equals(args[0])) {
                if (!sender.hasPermission(subCommand.permission))
                    Util.Message(sender, plugin.getMessageManager().getInstance().getNoPermission());
                else
                    subCommand.onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        Util.Message(sender, plugin.getMessageManager().getInstance().getNoSuchCommand());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (SubCommand subCommand : subCommands)
                if (sender.hasPermission(subCommand.permission))
                    result.add(subCommand.name);
            return result;
        }
        for (SubCommand subCommand : subCommands)
            if (subCommand.name.equals(args[0]) && sender.hasPermission(subCommand.permission))
                return subCommand.onTab(sender, Arrays.copyOfRange(args, 1, args.length));
        return null;
    }

    public void reload() {
        for (SubCommand subCommand : subCommands)
            subCommand.reloadMessage();
    }
}
