package org.celestialcraft.cCUtilities.modules.quests.bundle;

import org.celestialcraft.cCUtilities.modules.quests.model.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WeeklyBundle {
    private UUID bundleId;
    private UUID owner;
    private long expiresAtEpochSeconds;
    private List<Quest> quests = new ArrayList<>();

    public WeeklyBundle() {}

    public WeeklyBundle(UUID bundleId, UUID owner, long expiresAtEpochSeconds, List<Quest> quests) {
        this.bundleId = bundleId;
        this.owner = owner;
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
        this.quests = quests;
    }

    public UUID getBundleId() { return bundleId; }
    public UUID getOwner() { return owner; }
    public long getExpiresAtEpochSeconds() { return expiresAtEpochSeconds; }
    public List<Quest> getQuests() { return quests; }

    public void setBundleId(UUID bundleId) { this.bundleId = bundleId; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public void setExpiresAtEpochSeconds(long expiresAtEpochSeconds) { this.expiresAtEpochSeconds = expiresAtEpochSeconds; }
    public void setQuests(List<Quest> quests) { this.quests = quests; }

    public boolean isExpired(long nowEpochSeconds) {
        return nowEpochSeconds >= expiresAtEpochSeconds;
    }
}