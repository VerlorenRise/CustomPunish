/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.permission;

public class LimitResult {
    private final boolean configured;
    private final boolean unlimited;
    private final boolean canPermanent;
    private final long maxDurationMillis;
    private final String sourceInfo;

    public LimitResult(boolean configured, boolean unlimited, boolean canPermanent, long maxDurationMillis, String sourceInfo) {
        this.configured = configured;
        this.unlimited = unlimited;
        this.canPermanent = canPermanent;
        this.maxDurationMillis = maxDurationMillis;
        this.sourceInfo = sourceInfo;
    }

    public static LimitResult unlimited() {
        return new LimitResult(true, true, true, -1L, "Администратор (без ограничений)");
    }

    public static LimitResult limited(boolean canPermanent, long maxDurationMillis, String sourceInfo) {
        return new LimitResult(true, false, canPermanent, maxDurationMillis, sourceInfo);
    }

    public static LimitResult notConfigured() {
        return new LimitResult(false, false, false, 0L, "Лимит не задан");
    }

    public boolean isConfigured() {
        return this.configured;
    }

    public boolean isUnlimited() {
        return this.unlimited;
    }

    public boolean canPermanent() {
        return this.canPermanent;
    }

    public long getMaxDurationMillis() {
        return this.maxDurationMillis;
    }

    public String getSourceInfo() {
        return this.sourceInfo;
    }
}

