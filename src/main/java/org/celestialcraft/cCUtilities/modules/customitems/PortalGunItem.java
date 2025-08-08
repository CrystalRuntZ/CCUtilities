package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.customitems.portal.PortalData;
import org.celestialcraft.cCUtilities.modules.customitems.portal.PortalManager;

import java.util.*;

public class PortalGunItem implements CustomItem {

    private static final String LORE_LINE = "§7Portal Gun";
    private static final NamespacedKey UUID_KEY = new NamespacedKey(CCUtilities.getInstance(), "portal_uuid");

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "portal_gun";
    }

    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        for (Component line : Objects.requireNonNull(meta.lore())) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack gun = player.getInventory().getItemInMainHand();
        if (!matches(gun)) return;

        UUID id = getOrCreateUUID(gun);
        PortalData data = PortalManager.getOrCreate(id, player.getUniqueId());

        data.setRight(null);
        PortalManager.save();
        player.sendMessage("§cRight portal removed.");
    }

    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack gun = player.getInventory().getItemInMainHand();
        if (!matches(gun)) return;

        UUID id = getOrCreateUUID(gun);
        PortalData data = PortalManager.getOrCreate(id, player.getUniqueId());

        data.setLeft(null);
        PortalManager.save();
        player.sendMessage("§cLeft portal removed.");
    }

    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleClick(event, true);
    }

    public void onLeftClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleClick(event, false);
    }

    private void handleClick(PlayerInteractEvent event, boolean rightClick) {
        Player player = event.getPlayer();
        ItemStack gun = player.getInventory().getItemInMainHand();
        if (!matches(gun)) return;

        Block block = player.getTargetBlockExact(20);
        if (block == null || !block.getType().isSolid()) return;

        BlockFace face = event.getBlockFace();
        if (face == BlockFace.UP) return;

        UUID id = getOrCreateUUID(gun);
        PortalData data = PortalManager.getOrCreate(id, player.getUniqueId());

        Location loc = block.getRelative(face).getLocation().add(0.5, 0.5, 0.5);
        if (rightClick) data.setRight(loc);
        else data.setLeft(loc);

        PortalManager.save();
    }

    private UUID getOrCreateUUID(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        var container = meta.getPersistentDataContainer();
        String raw = container.get(UUID_KEY, PersistentDataType.STRING);

        UUID uuid;
        if (raw != null) {
            uuid = UUID.fromString(raw);
        } else {
            uuid = UUID.randomUUID();
            container.set(UUID_KEY, PersistentDataType.STRING, uuid.toString());
            item.setItemMeta(meta);
        }
        return uuid;
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        UUID uuid = player.getUniqueId();

        if (teleportCooldowns.containsKey(uuid) && System.currentTimeMillis() - teleportCooldowns.get(uuid) < 5000) return;

        for (PortalData portal : PortalManager.getPortalsForPlayer(uuid)) {
            if (!portal.hasBoth()) continue;

            Location left = portal.getLeft();
            Location right = portal.getRight();

            if (isClose(loc, left) && !isClose(loc, right)) {
                teleportTo(player, right);
                return;
            } else if (isClose(loc, right) && !isClose(loc, left)) {
                teleportTo(player, left);
                return;
            }
        }

        showParticles(player);
    }

    private void teleportTo(Player player, Location dest) {
        player.teleport(dest);
        teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isClose(Location a, Location b) {
        return a.getWorld().equals(b.getWorld()) && a.distanceSquared(b) < 1.5;
    }

    private void showParticles(Player player) {
        for (PortalData portal : PortalManager.getPortalsForPlayer(player.getUniqueId())) {
            if (portal.getLeft() != null) {
                player.spawnParticle(Particle.DUST, portal.getLeft(), 10, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.PURPLE, 1.5f));
            }
            if (portal.getRight() != null) {
                player.spawnParticle(Particle.DUST, portal.getRight(), 10, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.AQUA, 1.5f));
            }
        }
    }
}
