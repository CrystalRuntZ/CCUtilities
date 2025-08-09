package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

public class RubySlippersItem implements CustomItem {

    private static final String LORE_LINE = "§7Ruby Slippers";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    private static final NamespacedKey BED_WORLD = new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "ruby_bed_world");
    private static final NamespacedKey BED_X     = new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "ruby_bed_x");
    private static final NamespacedKey BED_Y     = new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "ruby_bed_y");
    private static final NamespacedKey BED_Z     = new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "ruby_bed_z");

    @Override
    public String getIdentifier() {
        return "ruby_slippers";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        var bed = event.getBed();
        var pdc = player.getPersistentDataContainer();

        pdc.set(BED_WORLD, PersistentDataType.STRING, Objects.requireNonNull(bed.getWorld()).getName());
        pdc.set(BED_X, PersistentDataType.INTEGER, bed.getX());
        pdc.set(BED_Y, PersistentDataType.INTEGER, bed.getY());
        pdc.set(BED_Z, PersistentDataType.INTEGER, bed.getZ());
    }

    // Skip PvP: do nothing if damage is by a player
    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Intentionally empty so PvP doesn't trigger the teleport logic
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player) return;

        ItemStack boots = player.getInventory().getBoots();
        if (!matches(boots)) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth > 0.0) return;

        // Lethal: cancel and teleport
        event.setCancelled(true);

        Location bedLoc = readBedLocation(player);
        if (bedLoc != null) {
            player.teleport(bedLoc);
        } else {
            World w = player.getWorld();
            player.teleport(w.getSpawnLocation());
        }
    }

    private Location readBedLocation(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String worldName = pdc.get(BED_WORLD, PersistentDataType.STRING);
        Integer x = pdc.get(BED_X, PersistentDataType.INTEGER);
        Integer y = pdc.get(BED_Y, PersistentDataType.INTEGER);
        Integer z = pdc.get(BED_Z, PersistentDataType.INTEGER);
        if (worldName == null || x == null || y == null || z == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(world, x + 0.5, y + 0.1, z + 0.5);
    }
}
