package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class ParticleWandItem implements CustomItem {

    private static final String LORE_LINE = "§7Rainbow Particle Wand";
    private static final long EFFECT_DURATION = 20 * 60 * 5; // 5 minutes in ticks
    private static final long INTERVAL_TICKS = 4; // 0.2s
    private static final long COOLDOWN_TICKS = 20 * 30; // 30s

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> activeEffects = new HashSet<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "particle_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        for (Component line : Objects.requireNonNull(meta.lore())) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;

        UUID uuid = target.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            event.getPlayer().sendMessage("§cThat player is on cooldown!");
            return;
        }

        if (activeEffects.contains(uuid)) {
            event.getPlayer().sendMessage("§cThat player already has an active particle effect!");
            return;
        }

        cooldowns.put(uuid, now + COOLDOWN_TICKS * 50);
        activeEffects.add(uuid);

        event.getPlayer().sendMessage("§aApplied swirling rainbow particles to " + target.getName() + "!");

        new BukkitRunnable() {
            long elapsed = 0;
            int hue = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline() || elapsed >= EFFECT_DURATION) {
                    activeEffects.remove(uuid);
                    this.cancel();
                    return;
                }

                float hueNormalized = (hue % 360) / 360f;
                int rgb = java.awt.Color.HSBtoRGB(hueNormalized, 1.0f, 1.0f) & 0xFFFFFF;
                Color color = Color.fromRGB(rgb);
                DustOptions dust = new DustOptions(color, 1.5f);

                Location center = p.getLocation().clone().add(0, 2.1, 0);
                double radius = 0.6;
                int points = 8;
                double angleOffset = (elapsed / (double) EFFECT_DURATION) * 2 * Math.PI;

                for (int i = 0; i < points; i++) {
                    double angle = angleOffset + ((2 * Math.PI) / points) * i;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location swirlPos = center.clone().add(x, 0, z);
                    p.getWorld().spawnParticle(Particle.DUST, swirlPos, 1, dust);
                }

                hue += 10;
                elapsed += INTERVAL_TICKS;
            }
        }.runTaskTimer(CCUtilities.getInstance(), 0, INTERVAL_TICKS);
    }
}
