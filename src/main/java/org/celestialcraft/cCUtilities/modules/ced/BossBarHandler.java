package org.celestialcraft.cCUtilities.modules.ced;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public class BossBarHandler {

    private final Map<DragonType, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Set<DragonType>> playerViewers = new HashMap<>();

    public void showBossBar(DragonType type, Component nameComponent, BarColor color, double baseHealth, double healthPerPlayer) {
        hideAllBossBars();

        // Debug logging
        Bukkit.getLogger().info("[CED] Showing boss bar for " + type.name());
        Bukkit.getLogger().info("[CED] -> Boss Bar Color: " + color);
        Bukkit.getLogger().info("[CED] -> Base Health: " + baseHealth);
        Bukkit.getLogger().info("[CED] -> Health Per Player: " + healthPerPlayer);

        String plainName = PlainTextComponentSerializer.plainText().serialize(nameComponent);
        BossBar bossBar = Bukkit.createBossBar(plainName, color, BarStyle.SOLID);
        bossBar.setVisible(true);
        bossBars.put(type, bossBar);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
            playerViewers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(type);
        }
    }

    public void updateProgress(DragonType type, double progress) {
        BossBar bar = bossBars.get(type);
        if (bar != null) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    public void updateVisiblePlayers(DragonType type) {
        BossBar bar = bossBars.get(type);
        if (bar == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Set<DragonType> types = playerViewers.computeIfAbsent(uuid, k -> new HashSet<>());
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
                types.add(type);
            }
        }
    }

    public void hideAllBossBars() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        bossBars.clear();
        playerViewers.clear();
    }
}
