/*
 * This file is part of ChatEx
 * Copyright (C) 2022 ChatEx Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package de.jeter.chatex.utils;

import de.jeter.chatex.ChatEx;
import de.jeter.chatex.plugins.PluginManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)&[0-9a-fk-or]|#[0-9a-f]{6}");
    private static final Pattern PREFIX_PLACEHOLDER_PATTERN = Pattern.compile("%prefix_(?:prefix|namecolor)%");

    public static String translateColorCodes(String string, Player p) {
        if (p.hasPermission("chatex.chat.color")) {
            return replaceColors(string);
        }
        if (COLOR_CODE_PATTERN.matcher(string).find()) {
            p.sendMessage(Locales.COMMAND_RESULT_NO_PERM.getString(p).replaceAll("%perm", "chatex.chat.color"));
        }
        return string;
    }

    public static String replaceColors(String message) {
        // Must run before translateGradientCodes: it matches "&#RRGGBB" (consuming the "&"),
        // whereas translateGradientCodes matches bare "#RRGGBB" regardless of a leading "&" and
        // would otherwise steal the hex code first, leaving a stray unconverted "&" behind.
        message = RGBColors.translateCustomColorCodes(message);
        message = RGBColors.translateGradientCodes(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<Player> getLocalRecipients(Player sender) {
        Location playerLocation = sender.getLocation();
        List<Player> recipients = new ArrayList<>();

        double squaredDistance = Math.pow(Config.RANGE.getInt(), 2);
        for (Player recipient : sender.getWorld().getPlayers()) {
            // Игнорируем игроков в режиме наблюдателя
            if (recipient.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }

            // Игнорируем игроков в ванише (EssentialsX)
            if (HookManager.checkEssentials()) {
                com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
                com.earth2me.essentials.User user = essentials.getUser(recipient);
                if (user != null && user.isVanished()) {
                    continue;
                }
            }
            if (Config.RANGE.getInt() > 0 && (playerLocation.distanceSquared(recipient.getLocation()) > squaredDistance)) {
                continue;
            }
            recipients.add(recipient);
        }

        return recipients;
    }

    public static String replacePlayerPlaceholders(Player player, String format) {
        return replacePlayerPlaceholders(player, format, player == null ? "%player" : player.getName());
    }

    public static String replacePlayerPlaceholders(Player player, String format, String playerPlaceholder) {
        if (player == null) {
            return format;
        }
        String result = format;

        List<String> protectedPlaceholders = new ArrayList<>();
        String placeholderMarker = "CHATEX_PREFIX_PLACEHOLDER_" + player.getUniqueId() + "_";
        Matcher prefixPlaceholderMatcher = PREFIX_PLACEHOLDER_PATTERN.matcher(result);
        StringBuffer protectedFormat = new StringBuffer();
        while (prefixPlaceholderMatcher.find()) {
            protectedPlaceholders.add(prefixPlaceholderMatcher.group());
            prefixPlaceholderMatcher.appendReplacement(
                    protectedFormat,
                    Matcher.quoteReplacement(placeholderMarker + (protectedPlaceholders.size() - 1))
            );
        }
        prefixPlaceholderMatcher.appendTail(protectedFormat);
        result = protectedFormat.toString();

        result = result.replace("%displayname", player.getDisplayName());
        result = result.replace("%prefix", PluginManager.getPrefix(player));
        result = result.replace("%suffix", PluginManager.getSuffix(player));
        result = result.replace("%player", playerPlaceholder);
        result = result.replace("%world", player.getWorld().getName());
        result = result.replace("%group", PluginManager.getGroupNames(player).length > 0 ? PluginManager.getGroupNames(player)[0] : "none");

        for (int i = 0; i < protectedPlaceholders.size(); i++) {
            result = result.replace(placeholderMarker + i, protectedPlaceholders.get(i));
        }

        if (HookManager.checkPlaceholderAPI()) {
            LogHelper.debug("PlaceholderAPI is installed! Replacing...");
            result = PlaceholderAPI.setPlaceholders(player, result);
            LogHelper.debug("Result: " + result);
        }

        if ((HookManager.checkEssentials() || HookManager.checkPurpur()) && Config.AFK_PLACEHOLDER.getBoolean()) {
            result = result.replace("%afk", "");
        }

        result = replaceColors(result);

        return result;
    }

    public static String escape(String string) {
        return string.replace("%", "%%");
    }

    public static boolean checkForBypassString(String message) {
        for (String block : Config.ADS_BYPASS.getStringList()) {
            if (message.toLowerCase().contains(block.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static void notifyOps(String msg) {
        for (Player op : ChatEx.getInstance().getServer().getOnlinePlayers()) {
            if (!op.hasPermission("chatex.notifyad")) {
                continue;
            }
            op.sendMessage(msg);
        }
    }

}
