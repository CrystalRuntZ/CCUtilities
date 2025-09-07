package org.celestialcraft.cCUtilities.modules.customenchants;

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
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LuckEnchant implements CustomEnchant {

    private static final String IDENTIFIER = "luck_enchant";
    private static final String RAW_LORE   = "&7Luck Enchant";

    private final Map<UUID, Integer> activeTasks = new HashMap<>();
    private JavaPlugin plugin;

    @Override public String getIdentifier() { return IDENTIFIER; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean appliesTo(ItemStack item) {
        // Applicable to ANY non-air item
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() {
        return true;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // Not used for this enchant
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return applyTo(item, false);
    }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    // ---- lifecycle hooks ----
    @Override public void onJoin(PlayerJoinEvent event) { startLuckCheck(event.getPlayer()); }
    @Override public void onQuit(PlayerQuitEvent event) { stopLuckCheck(event.getPlayer()); }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        // Defer a tick so the new slot item is in place
        Bukkit.getScheduler().runTaskLater(plugin, () -> startLuckCheck(event.getPlayer()), 1L);
    }

    @Override
    public void onHandSwap(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> startLuckCheck(player), 1L);
    }

    // ---- periodic effect manager ----
    private void startLuckCheck(Player player) {
        stopLuckCheck(player);
        if (!player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopLuckCheck(player);
                return;
            }

            boolean active = hasAnywhere(player);

            if (active) {
                // Keep LUCK refreshed; 40 ticks = 2s, run every 1s for headroom
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
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        if (player.isOnline()) player.removePotionEffect(PotionEffectType.LUCK);
    }

    private boolean hasAnywhere(Player p) {
        if (hasEnchant(p.getInventory().getItemInMainHand())) return true;
        if (hasEnchant(p.getInventory().getItemInOffHand()))  return true;
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) return true;
        }
        return false;
    }
}
