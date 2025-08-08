package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.awt.Color;
import java.util.*;

public class SombreroItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Set<UUID> tracked = new HashSet<>();

    public SombreroItem() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : tracked) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline() || player.isDead()) continue;
                    applySpeed(player);
                    spawnSwirlingParticles(player);
                }
            }
        }.runTaskTimer(CCUtilities.getInstance(), 0L, 10L);
    }

    @Override
    public String getIdentifier() {
        return "sombrero";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) return false;
        for (Component line : Objects.requireNonNull(item.getItemMeta().lore())) {
            if ("§7Sombrero".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    public void updateTracking(Player player) {
        if (hasSombreroItem(player)) {
            tracked.add(player.getUniqueId());
        } else {
            tracked.remove(player.getUniqueId());
            removeSpeed(player);
        }
    }

    public void onArmorChange(Player player, ItemStack newItem) {
        updateTracking(player);
        if (newItem != null && matches(newItem)) {
            playMaracaSound(player);
        }
    }

    public void onItemHeld(Player player) {
        updateTracking(player);
    }

    public void onHandSwap(Player player) {
        updateTracking(player);
    }

    private boolean hasSombreroItem(Player player) {
        List<ItemStack> items = List.of(
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand()
        );
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && matches(armor)) return true;
        }
        for (ItemStack item : items) {
            if (matches(item)) return true;
        }
        return false;
    }

    private void applySpeed(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1, true, false, false));
    }

    private void removeSpeed(Player player) {
        PotionEffect existing = player.getPotionEffect(PotionEffectType.SPEED);
        if (existing != null && existing.getAmplifier() == 1) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    private void spawnSwirlingParticles(Player player) {
        World world = player.getWorld();
        Location base = player.getLocation().add(0, 2.1, 0);
        double time = System.currentTimeMillis() / 200.0;

        double radius = 0.5;
        for (int i = 0; i < 3; i++) {
            double angle = time + (Math.PI * 2 * i / 3);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location loc = base.clone().add(x, 0, z);

            Color startColor, endColor;
            switch (i) {
                case 0 -> { // Green → White
                    startColor = Color.GREEN;
                    endColor = Color.WHITE;
                }
                case 1 -> { // White → Red
                    startColor = Color.WHITE;
                    endColor = Color.RED;
                }
                default -> { // Red → Green
                    startColor = Color.RED;
                    endColor = Color.GREEN;
                }
            }

            Particle.DustTransition transition = new Particle.DustTransition(
                    org.bukkit.Color.fromRGB(startColor.getRed(), startColor.getGreen(), startColor.getBlue()),
                    org.bukkit.Color.fromRGB(endColor.getRed(), endColor.getGreen(), endColor.getBlue()),
                    1.0f
            );

            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0, transition);
        }
    }

    private void playMaracaSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1.0f, 2.0f);
    }
}
