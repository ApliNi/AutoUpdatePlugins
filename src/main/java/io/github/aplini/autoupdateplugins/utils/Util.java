package io.github.aplini.autoupdateplugins.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Util {
    public static void Message(CommandSender player, String s) {
        if (player == null) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', s));
    }
}
