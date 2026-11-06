/*
 * Decompiled with CFR 0.152.
 */
package ru.projectn4.punish.model;

import java.util.UUID;
import ru.projectn4.punish.model.PunishmentType;

public class Punishment {
    private int id;
    private final UUID targetUuid;
    private final String targetName;
    private final String targetIp;
    private final PunishmentType type;
    private final UUID operatorUuid;
    private final String operatorName;
    private final String reason;
    private final long createdAt;
    private final long expiresAt;
    private boolean active;
    private UUID unpunishedByUuid;
    private String unpunishedByName;
    private String unpunishReason;
    private Long unpunishedAt;
    private boolean notifiedOffline = true;

    public Punishment(int id, UUID targetUuid, String targetName, String targetIp, PunishmentType type, UUID operatorUuid, String operatorName, String reason, long createdAt, long expiresAt, boolean active, UUID unpunishedByUuid, String unpunishedByName, String unpunishReason, Long unpunishedAt, boolean notifiedOffline) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetIp = targetIp;
        this.type = type;
        this.operatorUuid = operatorUuid;
        this.operatorName = operatorName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.unpunishedByUuid = unpunishedByUuid;
        this.unpunishedByName = unpunishedByName;
        this.unpunishReason = unpunishReason;
        this.unpunishedAt = unpunishedAt;
        this.notifiedOffline = notifiedOffline;
    }

    public Punishment(int id, UUID targetUuid, String targetName, String targetIp, PunishmentType type, UUID operatorUuid, String operatorName, String reason, long createdAt, long expiresAt, boolean active, UUID unpunishedByUuid, String unpunishedByName, String unpunishReason, Long unpunishedAt) {
        this(id, targetUuid, targetName, targetIp, type, operatorUuid, operatorName, reason, createdAt, expiresAt, active, unpunishedByUuid, unpunishedByName, unpunishReason, unpunishedAt, true);
    }

    public Punishment(UUID targetUuid, String targetName, String targetIp, PunishmentType type, UUID operatorUuid, String operatorName, String reason, long createdAt, long expiresAt) {
        this(-1, targetUuid, targetName, targetIp, type, operatorUuid, operatorName, reason, createdAt, expiresAt, true, null, null, null, null);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public String getTargetIp() {
        return this.targetIp;
    }

    public PunishmentType getType() {
        return this.type;
    }

    public UUID getOperatorUuid() {
        return this.operatorUuid;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public String getReason() {
        return this.reason;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getExpiresAt() {
        return this.expiresAt;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getUnpunishedByUuid() {
        return this.unpunishedByUuid;
    }

    public void setUnpunishedByUuid(UUID unpunishedByUuid) {
        this.unpunishedByUuid = unpunishedByUuid;
    }

    public String getUnpunishedByName() {
        return this.unpunishedByName;
    }

    public void setUnpunishedByName(String unpunishedByName) {
        this.unpunishedByName = unpunishedByName;
    }

    public String getUnpunishReason() {
        return this.unpunishReason;
    }

    public void setUnpunishReason(String unpunishReason) {
        this.unpunishReason = unpunishReason;
    }

    public Long getUnpunishedAt() {
        return this.unpunishedAt;
    }

    public void setUnpunishedAt(Long unpunishedAt) {
        this.unpunishedAt = unpunishedAt;
    }

    public boolean isPermanent() {
        return this.expiresAt <= 0L;
    }

    public boolean isExpired() {
        if (this.isPermanent()) {
            return false;
        }
        return System.currentTimeMillis() >= this.expiresAt;
    }

    public long getRemainingMillis() {
        if (this.isPermanent()) {
            return -1L;
        }
        long diff = this.expiresAt - System.currentTimeMillis();
        return Math.max(0L, diff);
    }

    public boolean isNotifiedOffline() {
        return this.notifiedOffline;
    }

    public void setNotifiedOffline(boolean notifiedOffline) {
        this.notifiedOffline = notifiedOffline;
    }
}

