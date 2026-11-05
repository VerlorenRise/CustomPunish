/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String name;
    private final String lastIp;
    private final long lastSeen;

    public PlayerData(UUID uuid, String name, String lastIp, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.lastIp = lastIp;
        this.lastSeen = lastSeen;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public String getLastIp() {
        return this.lastIp;
    }

    public long getLastSeen() {
        return this.lastSeen;
    }
}

