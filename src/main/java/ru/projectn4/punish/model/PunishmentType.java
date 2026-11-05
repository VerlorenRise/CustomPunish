/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.model;

public enum PunishmentType {
    BAN("БАН", "заблокирован"),
    BAN_IP("IP-БАН", "заблокирован по IP"),
    MUTE("МУТ", "замучен"),
    KICK("КИК", "исключен");

    private final String displayName;
    private final String actionName;

    private PunishmentType(String displayName, String actionName) {
        this.displayName = displayName;
        this.actionName = actionName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getActionName() {
        return this.actionName;
    }
}

