package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class BleedEnchant implements CustomEnchant {

    private static final String loreLine = "§7Bleed Effect";
    private static final double DAMAGE_MULTIPLIER = 1.2;
    private static final int BLEED_DURATION_TICKS = 100;
    private static final double BLEED_CHANCE = 0.2;

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Set<UUID> bleedingEntities = new HashSet<>();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "bleed_effect";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Material type = item.getType();
        return switch (type) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
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
        return "&7Bleed Effect";
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!hasEnchant(weapon)) return;

        UUID targetId = target.getUniqueId();

        if (bleedingEntities.contains(targetId)) {
            event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);
        }

        if (!bleedingEntities.contains(targetId) && Math.random() < BLEED_CHANCE) {
            bleedingEntities.add(targetId);
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10);
            startBleedTimer(targetId);
        }
    }

    private void startBleedTimer(UUID entityId) {
        int bleedTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // (optional) Add periodic damage here
        }, 0L, 1L).getTaskId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            bleedingEntities.remove(entityId);
            Bukkit.getScheduler().cancelTask(bleedTask);
        }, BLEED_DURATION_TICKS);
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

    private boolean hasEnchantAnywhere(Player player) {
        if (hasEnchant(player.getInventory().getItemInMainHand())) return true;
        if (hasEnchant(player.getInventory().getItemInOffHand())) return true;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) return true;
        }
        return false;
    }

    private void startEnchantCheck(Player player) {
        stopEnchantCheck(player);
        UUID uuid = player.getUniqueId();

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!hasEnchantAnywhere(player)) {
                stopEnchantCheck(player);
            }
        }, 0L, 40L).getTaskId();

        activeTasks.put(uuid, task);
    }

    private void stopEnchantCheck(Player player) {
        UUID uuid = player.getUniqueId();
        Integer task = activeTasks.remove(uuid);
        if (task != null) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }
}
