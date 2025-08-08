package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class StrengthOfTheEndEnchant implements CustomEnchant {
    private static final String loreLine = "§7Strength of the End";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "strength_of_the_end";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.getType().isItem()) return false;
        return switch (item.getType()) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD,
                 WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE,
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
        if (!(event.getDamager() instanceof LivingEntity damager)) return;

        ItemStack main = Objects.requireNonNull(damager.getEquipment()).getItemInMainHand();
        ItemStack off = damager.getEquipment().getItemInOffHand();
        if (!hasEnchant(main) && !hasEnchant(off)) return;

        Entity target = event.getEntity();
        if (target instanceof Enderman || target instanceof Endermite ||
                target instanceof EnderDragon || target instanceof Shulker) {
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
        return "&7Strength of the End";
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

    private void startEnchantCheck(Player player) {
        stopEnchantCheck(player);
        UUID uuid = player.getUniqueId();

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!hasEnchant(player.getInventory().getItemInMainHand()) &&
                    !hasEnchant(player.getInventory().getItemInOffHand())) {
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
