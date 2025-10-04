package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CelestialBucketItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Celestial Bucket";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> removalCooldown = new HashMap<>();
    private final Map<UUID, Long> placementCooldown = new HashMap<>();

    private static final String[] BLOCKED_WORLDS = {"shops", "spawnworld"};

    @Override
    public String getIdentifier() {
        return "celestial_bucket";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (isBlockedWorld(player.getWorld().getName())) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        if (player.getWorld().getEnvironment() == World.Environment.NETHER) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (player.isSneaking() &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (removalCooldown.containsKey(uuid) && now < removalCooldown.get(uuid)) {
                long remainingMs = removalCooldown.get(uuid) - now;
                player.sendActionBar(Component.text("§cWater removal cooldown: " + (remainingMs / 1000.0) + "s"));
                event.setCancelled(true);
                return;
            }

            Block targetBlock = getTargetBlock(player);
            if (targetBlock == null) return;

            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Block nearby = targetBlock.getLocation().add(x, y, z).getBlock();
                        if (nearby.getType() == Material.WATER) {
                            nearby.setType(Material.AIR);
                        }
                    }
                }
            }

            removalCooldown.put(uuid, now + 3000);
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (placementCooldown.containsKey(uuid) && now < placementCooldown.get(uuid)) {
                long remainingMs = placementCooldown.get(uuid) - now;
                player.sendActionBar(Component.text("§cWater placement cooldown: " + (remainingMs / 1000.0) + "s"));
                event.setCancelled(true);
                return;
            }

            Block targetBlock = getTargetBlock(player);
            if (targetBlock == null) return;

            Block placeBlock = targetBlock.getRelative(event.getBlockFace());
            if (placeBlock.getType().isAir()) {
                placeBlock.setType(Material.WATER);
            }

            placementCooldown.put(uuid, now + 2000);
            event.setCancelled(true);
        }
    }

    private boolean isBlockedWorld(String worldName) {
        for (String blocked : BLOCKED_WORLDS) {
            if (blocked.equalsIgnoreCase(worldName)) return true;
        }
        return false;
    }

    private Block getTargetBlock(Player player) {
        BlockIterator iterator = new BlockIterator(player, 5);
        while (iterator.hasNext()) {
            Block next = iterator.next();
            if (!next.getType().isAir()) {
                return next;
            }
        }
        return null;
    }
}
