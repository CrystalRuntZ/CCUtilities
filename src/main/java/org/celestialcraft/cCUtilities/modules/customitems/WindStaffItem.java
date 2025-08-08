package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.*;

public class WindStaffItem implements CustomItem {

    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize("§7Wind Staff");
    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether", "wild_the_end", "mapart");
    private final Map<UUID, Long> rightClickCooldown = new HashMap<>();
    private final Map<UUID, Long> sneakRightClickCooldown = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "wind_staff";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta() || !item.getItemMeta().hasLore())
            return false;
        var lore = item.getItemMeta().lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!ALLOWED_WORLDS.contains(player.getWorld().getName())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (rightClickCooldown.containsKey(uuid) && (now - rightClickCooldown.get(uuid) < 10_000)) return;
        rightClickCooldown.put(uuid, now);

        Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
        player.getWorld().spawnParticle(Particle.CLOUD, eye, 30, 0.3, 0.3, 0.3, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.2f);

        BlockIterator iter = new BlockIterator(player, 10);
        while (iter.hasNext()) {
            Location check = iter.next().getLocation();
            for (Entity e : player.getWorld().getNearbyEntities(check, 1, 1, 1)) {
                if (e instanceof LivingEntity target && !e.equals(player) && player.hasLineOfSight(e)) {
                    target.damage(2.0, player);
                    return;
                }
            }
        }
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!ALLOWED_WORLDS.contains(player.getWorld().getName())) return;
        if (!ClaimUtils.canBuild(player, player.getLocation())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (sneakRightClickCooldown.containsKey(uuid) && (now - sneakRightClickCooldown.get(uuid) < 3000)) return;
        sneakRightClickCooldown.put(uuid, now);

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize().multiply(1.5);

        ItemStack rodItem = new ItemStack(Material.STICK);
        try {
            rodItem.setType(Material.valueOf("BREEZE_ROD"));
        } catch (IllegalArgumentException ignored) {}

        Item thrown = player.getWorld().dropItem(eyeLoc, rodItem);
        thrown.setPickupDelay(20);
        thrown.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);
    }
}
