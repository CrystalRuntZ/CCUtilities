package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.*;

public class CyberParticlesEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Cyber Particles";
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    @Override public String getIdentifier() { return "cyber_particles"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // Allow on anything that isn't air
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() {
        // Mark this enchant as truly any-item (paper, sugarcane, etc.)
        return true;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    // Back-compat path
    @Override
    public ItemStack applyTo(ItemStack item) {
        LoreUtil.ensureLoreAfterTagBlock(item, RAW_LORE);
        return applyTo(item, false);
    }

    // Force-apply path (anvil uses this when allowed)
    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        // Write lore non-italic so it renders exactly as input
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override public void onJoin(PlayerJoinEvent event) { startEnchantCheck(event.getPlayer()); }
    @Override public void onQuit(PlayerQuitEvent event) { stopEnchantCheck(event.getPlayer()); }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> startEnchantCheck(event.getPlayer()), 1L);
    }

    @Override
    public void onHandSwap(Player player) {
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> startEnchantCheck(player), 1L);
    }

    private void startEnchantCheck(Player player) {
        stopEnchantCheck(player);
        if (!player.isOnline()) return;

        int task = Bukkit.getScheduler().runTaskTimer(CCUtilities.getInstance(), () -> {
            if (!hasEnchantAnywhere(player)) {
                stopEnchantCheck(player);
                return;
            }
            double angle = Math.toRadians(((double) System.currentTimeMillis() / 30) % 360);
            spawnParticles(player, angle);
        }, 0L, 2L).getTaskId();

        activeTasks.put(player.getUniqueId(), task);
    }

    private void stopEnchantCheck(Player player) {
        Integer task = activeTasks.remove(player.getUniqueId());
        if (task != null) Bukkit.getScheduler().cancelTask(task);
    }

    private boolean hasEnchantAnywhere(Player player) {
        if (hasEnchant(player.getInventory().getItemInMainHand())) return true;
        if (hasEnchant(player.getInventory().getItemInOffHand()))  return true;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) return true;
        }
        return false;
    }

    private void spawnParticles(Player player, double angle) {
        Color color = Color.fromRGB(128, 0, 255);
        DustOptions dust = new DustOptions(color, 1.5f);
        double radius = 1.5;
        int particleCount = 8;
        double heightFrequency = 0.2;

        for (int i = 0; i < particleCount; i++) {
            double particleAngle = angle + (2 * Math.PI * i / particleCount);
            double x = radius * Math.cos(particleAngle);
            double z = radius * Math.sin(particleAngle);
            double y = Math.sin(angle + i * heightFrequency) * 0.3;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 1 + y, z), 1, dust);
        }

        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1.5, 0), 3, 0.2, 0.3, 0.2, 0.01);
    }
}
