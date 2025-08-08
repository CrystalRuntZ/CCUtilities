package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class CelestialTeleporterItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Celestial Teleporter";
    private static final long COOLDOWN_MILLIS = 5 * 60 * 1000; // 5 min
    private static final Set<Material> UNSAFE_BLOCKS = EnumSet.of(
            Material.LAVA, Material.WATER, Material.CACTUS, Material.FIRE, Material.CAMPFIRE, Material.SOUL_FIRE
    );

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "celestial_teleporter";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        String formatted = LORE_IDENTIFIER.replace("&", "§");
        for (Component line : lore) {
            if (legacy.serialize(line).equalsIgnoreCase(formatted)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        // Allow main hand or offhand
        ItemStack item = (event.getHand() == EquipmentSlot.HAND)
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!matches(item)) return;
        if (!player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now < cooldowns.get(uuid)) {
            long seconds = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cCelestial Teleporter is recharging! " + seconds + "s remaining.");
            event.setCancelled(true);
            return;
        }

        Location origin = player.getLocation();
        int distance = 500 + random.nextInt(501); // 500–1000 blocks
        Vector direction = origin.getDirection().normalize().multiply(distance);
        Location target = origin.clone().add(direction);
        target.setY(255);

        Location safeLoc = null;
        for (int y = 255; y > 50; y--) {
            target.setY(y);
            Block ground = target.getBlock();
            Block above = target.clone().add(0, 1, 0).getBlock();
            Block above2 = target.clone().add(0, 2, 0).getBlock();

            if (ground.getType().isSolid() && !UNSAFE_BLOCKS.contains(ground.getType())
                    && above.getType() == Material.AIR && above2.getType() == Material.AIR) {
                safeLoc = ground.getLocation().add(0.5, 1, 0.5);
                break;
            }
        }

        if (safeLoc != null) {
            player.teleport(safeLoc);
            cooldowns.put(uuid, now + COOLDOWN_MILLIS);
            player.sendMessage("§bWhoosh! You were teleported " + distance + " blocks ahead!");
        } else {
            player.sendMessage("§cNo safe location found in that direction.");
        }

        event.setCancelled(true);
    }
}
