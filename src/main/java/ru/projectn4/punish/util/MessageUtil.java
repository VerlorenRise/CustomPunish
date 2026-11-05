/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.audience.Audience
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.command.CommandSender
 */
package ru.projectn4.punish.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private MessageUtil() {
    }

    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        if (message.contains("<") && message.contains(">")) {
            try {
                String converted = MessageUtil.convertLegacyToMiniMessage(message);
                return MINI_MESSAGE.deserialize(converted);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return LEGACY_SERIALIZER.deserialize(message);
    }

    public static List<Component> parseList(List<String> lines) {
        ArrayList<Component> components = new ArrayList<Component>(lines.size());
        for (String line : lines) {
            components.add(MessageUtil.parse(line));
        }
        return components;
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(MessageUtil.parse(message));
        }
    }

    public static void sendMessages(CommandSender sender, List<String> messages) {
        if (sender != null && messages != null) {
            for (String line : messages) {
                sender.sendMessage(MessageUtil.parse(line));
            }
        }
    }

    public static void sendMessage(Audience audience, String message) {
        if (audience != null && message != null && !message.isEmpty()) {
            audience.sendMessage(MessageUtil.parse(message));
        }
    }

    private static String convertLegacyToMiniMessage(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        return result.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>").replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>").replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>").replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>").replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>").replace("&r", "<reset>");
    }
}

