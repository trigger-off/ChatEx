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

import de.jeter.chatex.utils.Config;
import de.jeter.chatex.utils.Locales;
import de.jeter.chatex.utils.Utils;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class GroupChatService implements VoicechatPlugin, Listener, GroupChatSender {

    private volatile VoicechatServerApi serverApi;
    private BukkitVoicechatService registeredService;

    @Override
    public String getPluginId() {
        return "chatex_group_chat";
    }

    @Override
    public void initialize(VoicechatApi api) {
        serverApi = api instanceof VoicechatServerApi ? (VoicechatServerApi) api : null;
        if (serverApi == null) {
            ChatEx.getInstance().getLogger().warning("Simple Voice Chat initialized without a server API; group chat is unavailable.");
        } else {
            ChatEx.getInstance().getLogger().info("Hooked into the Simple Voice Chat server API.");
        }
    }

    public void registerWithVoicechat() {
        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            serverApi = null;
            registeredService = null;
            ChatEx.getInstance().getLogger().warning("Simple Voice Chat is not available; group chat is disabled.");
            return;
        }
        if (service == registeredService) {
            return;
        }
        registeredService = service;
        serverApi = null;
        service.registerPlugin(this);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (isVoicechatPlugin(event.getPlugin())) {
            registerWithVoicechat();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (isVoicechatPlugin(event.getPlugin())) {
            serverApi = null;
            registeredService = null;
        }
    }

    public void shutdown() {
        serverApi = null;
        registeredService = null;
    }

    @Override
    public void sendGroupMessage(Player sender, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(ChatEx.getInstance(), () -> sendGroupMessage(sender, message));
            return;
        }
        if (sender == null || !sender.isOnline()) {
            return;
        }
        if (!sender.hasPermission("chatex.allowchat")) {
            sender.sendMessage(Locales.COMMAND_RESULT_NO_PERM.getString(sender)
                    .replaceAll("%perm", "chatex.allowchat"));
            return;
        }
        if (!sender.hasPermission("chatex.groupchat.use")) {
            sendNotice(sender, Config.GROUP_CHAT_NO_PERMISSION.getString());
            return;
        }
        if (message == null || message.trim().isEmpty()) {
            sendNotice(sender, Config.GROUP_CHAT_EMPTY_MESSAGE.getString());
            return;
        }
        if (!Config.GROUP_CHAT_ENABLED.getBoolean()) {
            sendNotice(sender, Config.GROUP_CHAT_DISABLED.getString());
            return;
        }

        VoicechatServerApi api = serverApi;
        if (api == null) {
            sendNotice(sender, Config.GROUP_CHAT_UNAVAILABLE.getString());
            return;
        }

        VoicechatConnection senderConnection = api.getConnectionOf(sender.getUniqueId());
        if (!isConnected(senderConnection) || !senderConnection.isInGroup() || senderConnection.getGroup() == null) {
            sendNotice(sender, Config.GROUP_CHAT_NOT_IN_GROUP.getString());
            return;
        }

        UUID groupId = senderConnection.getGroup().getId();
        Component formattedMessage = formatMessage(sender, message);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            VoicechatConnection recipientConnection = api.getConnectionOf(recipient.getUniqueId());
            if (!isConnected(recipientConnection) || !recipientConnection.isInGroup()) {
                continue;
            }
            Group recipientGroup = recipientConnection.getGroup();
            if (recipientGroup != null && groupId.equals(recipientGroup.getId())) {
                recipient.sendMessage(formattedMessage);
            }
        }
    }

    private boolean isConnected(VoicechatConnection connection) {
        return connection != null && connection.isConnected();
    }

    private Component formatMessage(Player sender, String message) {
        String format = Utils.replacePlayerPlaceholders(sender, Config.GROUP_CHAT_FORMAT.getString());
        format = format.replace("%prefix_prefix%", "").replace("%prefix_namecolor%", "");
        format = format.replace("%message", Utils.translateColorCodes(message, sender));

        return LegacyComponentSerializer.legacySection().deserialize(format);
    }

    private void sendNotice(Player player, String message) {
        player.sendMessage(Utils.replaceColors(message));
    }

    private boolean isVoicechatPlugin(Plugin plugin) {
        String name = plugin.getName();
        return "voicechat".equalsIgnoreCase(name) || "Simple Voice Chat".equalsIgnoreCase(name);
    }
}
