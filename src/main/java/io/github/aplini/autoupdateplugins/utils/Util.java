package io.github.aplini.autoupdateplugins.utils;

import com.google.gson.Gson;
import okhttp3.Response;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static void Message(CommandSender player, String s)
    {
        if (player == null) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', s));
    }
}
