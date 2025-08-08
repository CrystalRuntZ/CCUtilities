package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CyberParticlesEnchant implements CustomEnchant {

    private static final String loreLine = "§7Cyber Particles";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "cyber_particles";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (serializer.serialize(line).equals(loreLine)) return true;
        }
        return false;
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (!appliesTo(item) || hasEnchant(item)) return item;
        var meta = item.getItemMeta();
        List<Component> existingLore = meta.lore();
        List<Component> lore = (existingLore != null) ? new ArrayList<>(existingLore) : new ArrayList<>();
        lore.add(serializer.deserialize(loreLine));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getLoreLine() {
        return "&7Cyber Particles";
    }

    @Override
    public void onJoin(PlayerJoinEvent event) {
        startEnchantCheck(event.getPlayer());
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        stopEnchantCheck(event.getPlayer());
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> startEnchantCheck(event.getPlayer()), 1L);
    }

    @Override
    public void onHandSwap(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> startEnchantCheck(player), 1L);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // No damage effects for this enchant
    }

    private void startEnchantCheck(Player player) {
        stopEnchantCheck(player);
        UUID uuid = player.getUniqueId();

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!hasEnchantAnywhere(player)) {
                stopEnchantCheck(player);
                return;
            }

            double angle = Math.toRadians((System.currentTimeMillis() / 30) % 360);
            spawnParticles(player, angle);

        }, 0L, 2L).getTaskId();

        activeTasks.put(uuid, task);
    }

    private void stopEnchantCheck(Player player) {
        UUID uuid = player.getUniqueId();
        Integer task = activeTasks.remove(uuid);
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }

    private boolean hasEnchantAnywhere(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (hasEnchant(main) || hasEnchant(off)) return true;
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
