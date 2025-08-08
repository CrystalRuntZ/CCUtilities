package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PocketSandItem implements CustomItem {

    private static final String LORE_LINE = "§7Pocket Sand";
    private static final long COOLDOWN_MS = 60_000;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "pocket_sand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        player.getWorld().spawnParticle(
                Particle.BLOCK_CRUMBLE,
                player.getEyeLocation(),
                100,
                1.0,
                1.0,
                1.0,
                Material.SAND.createBlockData()
        );

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SAND_BREAK, 1.0f, 1.0f);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            if (!target.getWorld().equals(player.getWorld())) continue;
            if (target.getLocation().distance(player.getLocation()) <= 15) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 15, 0));
            }
        }

        event.setCancelled(true);
    }
}
