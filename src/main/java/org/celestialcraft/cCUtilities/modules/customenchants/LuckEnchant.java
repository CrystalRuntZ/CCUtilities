package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class LuckEnchant implements CustomEnchant {
    private static final String IDENTIFIER = "luck_enchant";
    private static final String loreLine = "§7Luck Enchant";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
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
            String plain = serializer.serialize(line);
            if (plain.contains("Luck Enchant")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // Not used for this enchant
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
        return "&7Luck Enchant";
    }

    @Override
    public void onJoin(PlayerJoinEvent event) {
        startLuckCheck(event.getPlayer());
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        stopLuckCheck(event.getPlayer());
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> startLuckCheck(event.getPlayer()), 1L);
    }

    @Override
    public void onHandSwap(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> startLuckCheck(player), 1L);
    }

    private void startLuckCheck(Player player) {
        stopLuckCheck(player);
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopLuckCheck(player);
                return;
            }

            boolean has = false;

            if (hasEnchant(player.getInventory().getItemInMainHand())) {
                has = true;
            } else if (hasEnchant(player.getInventory().getItemInOffHand())) {
                has = true;
            } else {
                for (ItemStack armor : player.getInventory().getArmorContents()) {
                    if (hasEnchant(armor)) {
                        has = true;
                        break;
                    }
                }
            }

            if (has) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, 0, true, false, false));
            } else {
                player.removePotionEffect(PotionEffectType.LUCK);
                stopLuckCheck(player);
            }
        }, 0L, 20L).getTaskId();

        activeTasks.put(uuid, task);
    }

    private void stopLuckCheck(Player player) {
        UUID uuid = player.getUniqueId();
        Integer taskId = activeTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.LUCK);
        }
    }
}
