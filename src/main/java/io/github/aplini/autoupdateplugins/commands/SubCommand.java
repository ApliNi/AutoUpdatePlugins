package io.github.aplini.autoupdateplugins.commands;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
public abstract class SubCommand {
    public abstract void onCommand(Player player, String[] args);
    public abstract List<String> onTab(Player player, String[] args);
    public abstract void reloadMessage();
    public String name;
    public String permission;
    public String usage;
    public String description;
}
