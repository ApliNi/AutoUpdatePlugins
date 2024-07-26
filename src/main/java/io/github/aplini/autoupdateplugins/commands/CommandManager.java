package io.github.aplini.autoupdateplugins.commands;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.commands.subcommands.reload;
import io.github.aplini.autoupdateplugins.commands.subcommands.update;
import io.github.aplini.autoupdateplugins.utils.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
    public final ArrayList<SubCommand> subCommands = new ArrayList<>();
    private final AutoUpdate plugin;

    public CommandManager(AutoUpdate plugin) {
        this.plugin = plugin;
        subCommands.add(new reload(plugin));
        subCommands.add(new update());
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
                    subCommand.onCommand((Player) sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        Util.Message(sender, plugin.getMessageManager().getInstance().getNoSuchCommand());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return null;
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (SubCommand subCommand : subCommands)
                if (sender.hasPermission(subCommand.permission))
                    result.add(subCommand.name);
            return result;
        }
        for (SubCommand subCommand : subCommands)
            if (subCommand.name.equals(args[0]) && sender.hasPermission(subCommand.permission))
                return subCommand.onTab((Player) sender, Arrays.copyOfRange(args, 1, args.length));
        return null;
    }

    public void reload() {
        for (SubCommand subCommand : subCommands)
            subCommand.reloadMessage();
    }
}
