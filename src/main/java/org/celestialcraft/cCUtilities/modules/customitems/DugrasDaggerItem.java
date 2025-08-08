package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class DugrasDaggerItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final String IDENTIFIER = "dugras_dagger";
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> activeTasks = new HashMap<>();

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if ("§7Dugra's Dagger".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startStrengthCheck(player);
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        stopStrengthCheck(event.getPlayer());
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> startStrengthCheck(player), 1L);
    }

    public void onHandSwap(Player player) {
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> startStrengthCheck(player), 1L);
    }

    private void startStrengthCheck(Player player) {
        stopStrengthCheck(player); // Clear existing task

        UUID uuid = player.getUniqueId();
        int task = Bukkit.getScheduler().runTaskTimer(CCUtilities.getInstance(), () -> {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (matches(main)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false));
            } else {
                player.removePotionEffect(PotionEffectType.STRENGTH);
                stopStrengthCheck(player);
            }
        }, 0L, 20L).getTaskId();

        activeTasks.put(uuid, task);
    }

    private void stopStrengthCheck(Player player) {
        Integer taskId = activeTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!matches(weapon)) return;

        UUID uuid = player.getUniqueId();
        long last = cooldowns.getOrDefault(uuid, 0L);
        if ((System.currentTimeMillis() - last) < 30_000) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 0));
        cooldowns.put(uuid, System.currentTimeMillis());
    }
}
