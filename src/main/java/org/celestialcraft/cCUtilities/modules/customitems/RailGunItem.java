package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class RailGunItem implements CustomItem {

    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize("§7Rail Gun");
    private static final NamespacedKey SHOOTER_LOCATION_KEY =
            new NamespacedKey(CCUtilities.getInstance(), "railgun_shooter_location");

    @Override
    public String getIdentifier() {
        return "rail_gun_item";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof org.bukkit.entity.Player player)) return;
        ItemStack bow = player.getInventory().getItemInMainHand();
        if (!matches(bow)) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;

        Location shooterLocation = player.getLocation();
        String formatted = formatLocation(shooterLocation);
        arrow.getPersistentDataContainer().set(SHOOTER_LOCATION_KEY, PersistentDataType.STRING, formatted);
        arrow.setPersistent(true);
    }

    @Override
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof AbstractArrow arrow)) return;

        PersistentDataContainer container = arrow.getPersistentDataContainer();
        if (!container.has(SHOOTER_LOCATION_KEY, PersistentDataType.STRING)) return;

        String raw = container.get(SHOOTER_LOCATION_KEY, PersistentDataType.STRING);
        if (raw == null) return;

        Location shooterLocation = parseLocation(arrow.getWorld(), raw);
        if (shooterLocation == null) return;

        Location victimLocation = event.getEntity().getLocation();
        double distance = shooterLocation.distance(victimLocation);
        double extra = distance / 3.0;
        event.setDamage(event.getDamage() + extra);
    }

    private String formatLocation(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }

    private Location parseLocation(World world, String s) {
        String[] parts = s.split(",");
        if (parts.length != 3) return null;
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
