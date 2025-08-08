package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.time.Duration;
import java.util.*;

public class RailPlacerItem implements CustomItem {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final NamespacedKey key = new NamespacedKey(CCUtilities.getInstance(), "rail_type");

    public enum RailType {
        NORMAL("Normal Rail", Material.RAIL),
        POWERED("Powered Rail", Material.POWERED_RAIL),
        ACTIVATOR("Activator Rail", Material.ACTIVATOR_RAIL),
        DETECTOR("Detector Rail", Material.DETECTOR_RAIL);

        private final String displayName;
        private final Material material;

        RailType(String displayName, Material material) {
            this.displayName = displayName;
            this.material = material;
        }

        public RailType next() {
            RailType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getMaterial() {
            return material;
        }

        public static RailType fromString(String name) {
            for (RailType type : values()) {
                if (type.name().equalsIgnoreCase(name)) return type;
            }
            return NORMAL;
        }
    }

    @Override
    public String getIdentifier() {
        return "rail_placer";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta() || item.getItemMeta().lore() == null)
            return false;

        return Objects.requireNonNull(item.getItemMeta().lore()).stream()
                .anyMatch(line -> line != null && line.toString().contains("Rail Placer"));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!ClaimUtils.canBuild(player)) return;
        String world = player.getWorld().getName().toLowerCase();
        if (world.equals("spawnworld") || world.equals("shops")) return;

        ItemStack item = event.getItem();
        if (item == null || !matches(item)) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> {
                if (hasCooldown(player)) return;
                setCooldown(player);

                RailType current = getRailType(item);
                RailType next = current.next();
                setRailType(item, next);

                Title title = Title.title(
                        Component.text("Rail Type", NamedTextColor.GRAY),
                        Component.text(next.getDisplayName()).color(TextColor.fromHexString("#c1adfe")),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(200))
                );
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                event.setCancelled(true);
            }
            case RIGHT_CLICK_BLOCK -> {
                if (hasCooldown(player)) return;
                setCooldown(player);

                Block targetBlock = event.getClickedBlock();
                if (targetBlock == null) return;
                Block placeBlock = targetBlock.getRelative(event.getBlockFace());

                if (!placeBlock.getType().isAir()) return;

                RailType selected = getRailType(item);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        placeBlock.setType(selected.getMaterial());
                        player.playSound(placeBlock.getLocation(), Sound.BLOCK_METAL_PLACE, 1f, 1f);
                    }
                }.runTask(CCUtilities.getInstance());
                event.setCancelled(true);
            }
            default -> {}
        }
    }

    private RailType getRailType(ItemStack item) {
        if (!item.hasItemMeta()) return RailType.NORMAL;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return RailType.NORMAL;

        String stored = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return stored != null ? RailType.fromString(stored) : RailType.NORMAL;
    }

    private void setRailType(ItemStack item, RailType type) {
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
    }

    private boolean hasCooldown(Player player) {
        Long last = cooldowns.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < 1000L;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
