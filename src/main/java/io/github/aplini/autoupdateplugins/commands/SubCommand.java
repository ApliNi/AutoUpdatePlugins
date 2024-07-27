package io.github.aplini.autoupdateplugins.commands;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
public abstract class SubCommand {
    public String name;
    public String permission;
    public String usage;
    public String description;

    public abstract void onCommand(CommandSender sender, String[] args);

    public abstract List<String> onTab(CommandSender sender, String[] args);

    public abstract void reloadMessage();
}
