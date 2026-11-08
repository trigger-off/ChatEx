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
package de.jeter.chatex;

import de.jeter.chatex.plugins.PluginManager;
import de.jeter.chatex.utils.*;
import de.jeter.updatechecker.SpigotUpdateChecker;
import de.jeter.updatechecker.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatEx extends JavaPlugin implements Listener {

    private static ChatEx INSTANCE;
    private UpdateChecker updatechecker = null;
    private ChatListener chatListener;
    private GroupChatSender groupChatSender;
    private Object groupChatIntegration;

    public static ChatEx getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        Config.load();
        Locales.load();
        PluginManager.load();
        ChatLogger.load();
        RGBColors.load();
        DomainDictionary.load();

        chatListener = new ChatListener();
        chatListener.register(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("chatex").setExecutor(new CommandHandler());
        getCommand("gc").setExecutor(new GroupChatCommand(this));
        getCommand("gc").setTabCompleter(new GroupChatCommand.TabCompleterImpl());
        initializeGroupChatIntegration();

        if (Config.CHECK_UPDATE.getBoolean()) {
            updatechecker = new SpigotUpdateChecker(this, 71041);
        }

        ChannelHandler.load();

        if (Config.B_STATS.getBoolean()) {
            Metrics metrics = new Metrics(this, 7744);
            CustomCharts.addPermissionsPluginChart(metrics);
            CustomCharts.addUpdateCheckerChart(metrics);
            getLogger().info("Thanks for using bstats, it was enabled!");
        }

        getLogger().info("Is now enabled!");
    }

    @Override
    public void onDisable() {
        shutdownGroupChatIntegration();
        AntiSpamManager.getInstance().clear();
        ChatLogger.close();
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("Is now disabled!");
    }

    public UpdateChecker getUpdateChecker() {
        return this.updatechecker;
    }

    public ChatListener getChatListener() {
        return this.chatListener;
    }

    public void sendGroupMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, () -> sendGroupMessage(sender, message));
            return;
        }
        if (groupChatSender != null) {
            groupChatSender.sendGroupMessage(sender, message);
            return;
        }
        if (sender == null || !sender.isOnline()) {
            return;
        }
        if (!sender.hasPermission("chatex.allowchat")) {
            sender.sendMessage(Locales.COMMAND_RESULT_NO_PERM.getString(sender).replaceAll("%perm", "chatex.allowchat"));
            return;
        }
        if (!sender.hasPermission("chatex.groupchat.use")) {
            sender.sendMessage(Config.GROUP_CHAT_NO_PERMISSION.getString());
        } else if (message == null || message.trim().isEmpty()) {
            sender.sendMessage(Config.GROUP_CHAT_EMPTY_MESSAGE.getString());
        } else if (!Config.GROUP_CHAT_ENABLED.getBoolean()) {
            sender.sendMessage(Config.GROUP_CHAT_DISABLED.getString());
        } else {
            sender.sendMessage(Config.GROUP_CHAT_UNAVAILABLE.getString());
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (isVoicechatPlugin(event.getPlugin()) && groupChatSender == null) {
            initializeGroupChatIntegration();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (isVoicechatPlugin(event.getPlugin())) {
            shutdownGroupChatIntegration();
        }
    }

    private void initializeGroupChatIntegration() {
        if (groupChatSender != null) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("voicechat") == null
                && getServer().getPluginManager().getPlugin("Simple Voice Chat") == null) {
            getLogger().warning("Simple Voice Chat is not available; group chat is disabled.");
            return;
        }
        try {
            Class<?> integrationClass = Class.forName(
                    "de.jeter.chatex.GroupChatService",
                    true,
                    getClassLoader()
            );
            Object integration = integrationClass.getConstructor().newInstance();
            getServer().getPluginManager().registerEvents((Listener) integration, this);
            integrationClass.getMethod("registerWithVoicechat").invoke(integration);
            groupChatIntegration = integration;
            groupChatSender = (GroupChatSender) integration;
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            getLogger().severe("Could not initialize Simple Voice Chat integration: " + exception.getMessage());
        }
    }

    private void shutdownGroupChatIntegration() {
        if (groupChatIntegration != null) {
            try {
                groupChatIntegration.getClass().getMethod("shutdown").invoke(groupChatIntegration);
            } catch (ReflectiveOperationException exception) {
                getLogger().warning("Could not shut down Simple Voice Chat integration cleanly: " + exception.getMessage());
            }
        }
        groupChatIntegration = null;
        groupChatSender = null;
    }

    private boolean isVoicechatPlugin(Plugin plugin) {
        String name = plugin.getName();
        return "voicechat".equalsIgnoreCase(name) || "Simple Voice Chat".equalsIgnoreCase(name);
    }
}
