/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.config;

public class ConfigLimitGroup {
    private final String name;
    private final String permission;
    private final long maxDurationMillis;
    private final boolean canPermanent;

    public ConfigLimitGroup(String name, String permission, long maxDurationMillis, boolean canPermanent) {
        this.name = name;
        this.permission = permission;
        this.maxDurationMillis = maxDurationMillis;
        this.canPermanent = canPermanent;
    }

    public String getName() {
        return this.name;
    }

    public String getPermission() {
        return this.permission;
    }

    public long getMaxDurationMillis() {
        return this.maxDurationMillis;
    }

    public boolean canPermanent() {
        return this.canPermanent;
    }
}

