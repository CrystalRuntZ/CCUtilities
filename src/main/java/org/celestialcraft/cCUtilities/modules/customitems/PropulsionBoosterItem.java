package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class PropulsionBoosterItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Propulsion Booster";
    private static final long COOLDOWN_TICKS = 40L;

    private final Set<UUID> cooldowns = new HashSet<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "propulsion_booster";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> serializer.serialize(line).equals(LORE_IDENTIFIER));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;
        if (!player.isGliding()) return;

        UUID uuid = player.getUniqueId();
        if (cooldowns.contains(uuid)) return;

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(player.getVelocity().add(direction.multiply(1.5)));

        cooldowns.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldowns.remove(uuid);
            }
        }.runTaskLater(JavaPlugin.getPlugin(CCUtilities.class), COOLDOWN_TICKS);

        event.setCancelled(true);
    }
}
