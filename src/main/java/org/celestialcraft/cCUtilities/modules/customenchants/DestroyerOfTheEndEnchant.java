package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DestroyerOfTheEndEnchant implements CustomEnchant {
    private static final String loreLine = "§7Destroyer of the End";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "destroyer_of_the_end";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Material type = item.getType();
        return switch (type) {
            case DIAMOND_SWORD, NETHERITE_SWORD, IRON_SWORD, STONE_SWORD, WOODEN_SWORD,
                 DIAMOND_AXE, NETHERITE_AXE, IRON_AXE, STONE_AXE, WOODEN_AXE,
                 BOW, CROSSBOW, MACE -> true;
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
    public void applyEffect(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof LivingEntity living)) return;

        ItemStack weapon = living.getEquipment() != null ? living.getEquipment().getItemInMainHand() : null;
        if (!hasEnchant(weapon)) return;

        Entity target = event.getEntity();
        if (target.getWorld().getEnvironment() != World.Environment.THE_END) return;

        if (isOnMainIsland(target)) {
            event.setDamage(event.getDamage() * 1.5);
        }
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
        return "&7Destroyer of the End";
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
        if (!player.isOnline()) return;
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

    private boolean isOnMainIsland(Entity entity) {
        return entity.getLocation().distanceSquared(entity.getWorld().getSpawnLocation()) <= 500 * 500;
    }
}
