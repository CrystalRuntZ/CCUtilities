package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PotionOfTheEndItem implements CustomItem {
    private static final String IDENTIFIER = "potion_of_the_end";
    private static final String LORE_LINE = "§7Potion of the End";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Set<UUID> boostedPlayers = new HashSet<>();

    private final Plugin plugin;

    public PotionOfTheEndItem(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!matches(event.getItem())) return;

        long now = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUsed < 15 * 60 * 1000L) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou must wait before using another Potion of the End."));
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        boostedPlayers.add(player.getUniqueId());
        player.sendMessage(Component.text("§dYou feel the power of the End surge through you."));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boostedPlayers.remove(player.getUniqueId());
            player.sendMessage(Component.text("§7The effect of the Potion of the End has worn off."));
        }, 20L * 60 * 15);
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (!boostedPlayers.contains(player.getUniqueId())) return;

        event.setDamage(event.getDamage() * 1.5);
    }
}
