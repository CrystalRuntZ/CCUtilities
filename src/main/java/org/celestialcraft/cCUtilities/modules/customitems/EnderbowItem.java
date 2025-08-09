package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnderbowItem implements CustomItem {

    private static final String LORE_LINE = "§7Enderbow";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    // Tracks arrow -> shooter
    private final Map<UUID, UUID> arrowShooters = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "enderbow";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        ProjectileSource shooter = proj.getShooter();
        if (!(shooter instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (!matches(bow)) return;

        arrowShooters.put(proj.getUniqueId(), player.getUniqueId());
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        UUID shooterId = arrowShooters.remove(proj.getUniqueId());
        if (shooterId == null) return;

        Player player = proj.getServer().getPlayer(shooterId);
        if (player == null) return;

        Location hitLoc = event.getHitBlock() != null ? event.getHitBlock().getLocation().add(0.5, 1, 0.5)
                : event.getEntity().getLocation();

        player.teleport(hitLoc);
    }
}
