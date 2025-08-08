package org.celestialcraft.cCUtilities.modules.quests.model;

import java.util.UUID;

public class Quest {

    private final UUID id;
    private final String questId;
    private final QuestType type;
    private final int target;
    private int progress;
    private final long startTime;
    private final long expirationSeconds;
    private final long claimWindowSeconds;
    private final String targetItem;

    public Quest(UUID id, String questId, QuestType type, int target, long expirationSeconds, long claimWindowSeconds, String targetItem) {
        this(id, questId, type, target, 0, System.currentTimeMillis(), expirationSeconds, claimWindowSeconds, targetItem);
    }

    public Quest(UUID id, String questId, QuestType type, int target, long expirationSeconds, long claimWindowSeconds) {
        this(id, questId, type, target, 0, System.currentTimeMillis(), expirationSeconds, claimWindowSeconds, null);
    }

    public Quest(UUID id, String questId, QuestType type, int target, int progress, long startTime, long expirationSeconds, long claimWindowSeconds, String targetItem) {
        this.id = id;
        this.questId = questId;
        this.type = type;
        this.target = target;
        this.progress = progress;
        this.startTime = startTime;
        this.expirationSeconds = expirationSeconds;
        this.claimWindowSeconds = claimWindowSeconds;
        this.targetItem = targetItem;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > startTime + expirationSeconds * 1000;
    }

    public boolean canClaim() {
        long now = System.currentTimeMillis();
        long endTime = startTime + expirationSeconds * 1000;
        return now >= endTime && now <= endTime + claimWindowSeconds * 1000;
    }

    public boolean isComplete() {
        return progress >= target;
    }

    public UUID getId() {
        return id;
    }

    public String getQuestId() {
        return questId;
    }

    public QuestType getType() {
        return type;
    }

    public int getTarget() {
        return target;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public long getClaimWindowSeconds() {
        return claimWindowSeconds;
    }

    public String getTargetItem() {
        return targetItem;
    }
}
