package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;

import java.util.*;

public class BrazilianBrewItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final String IDENTIFIER = "brazilian_brew";
    private final Set<UUID> cooldownPlayers = new HashSet<>();
    private final Random random = new Random();

    public BrazilianBrewItem(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack main = player.getInventory().getItemInMainHand();
                    ItemStack off = player.getInventory().getItemInOffHand();
                    if ((matches(main) || matches(off)) && !cooldownPlayers.contains(player.getUniqueId())) {
                        applySpeedEffect(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // Every 30 seconds
    }

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
            if ("§7Brazilian Brew".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    private void applySpeedEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 254, true, true, true));
        spawnWhiteDust(player);
        cooldownPlayers.add(player.getUniqueId());

        long delay = 20L * (120 + random.nextInt(180)); // 2–5 minutes
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), () -> cooldownPlayers.remove(player.getUniqueId()), delay);
    }

    private void spawnWhiteDust(Player player) {
        var dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2.0f);
        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 1, 0),
                60,
                0.5, 0.5, 0.5,
                0.01,
                dustOptions
        );
    }
}
