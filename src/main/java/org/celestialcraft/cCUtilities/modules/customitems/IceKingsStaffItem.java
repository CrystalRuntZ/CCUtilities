package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;

public class IceKingsStaffItem implements CustomItem {

    private static final String LORE_LINE = "&7Ice King's Staff";
    private static final List<Material> ICE_TYPES = List.of(Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE);
    private static final String REQUIRED_WORLD = "wild";

    private final Map<UUID, Integer> selectedIndex = new HashMap<>();
    private final Map<UUID, Long> lastPlaceTime = new HashMap<>();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "ice_kings_staff";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        return Objects.requireNonNull(meta.lore()).stream().anyMatch(line ->
                legacy.serialize(line).equalsIgnoreCase(LORE_LINE.replace("&", "§")));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase(REQUIRED_WORLD)) return;
        if (!matches(player.getInventory().getItemInMainHand())) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            handleLeftClick(player);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            handleRightClick(player, event.getClickedBlock(), event.getBlockFace());
        }
    }

    private void handleLeftClick(Player player) {
        UUID uuid = player.getUniqueId();
        int index = selectedIndex.getOrDefault(uuid, 0);
        index = (index + 1) % ICE_TYPES.size();
        selectedIndex.put(uuid, index);

        Material selected = ICE_TYPES.get(index);
        String name = selected.name().replace("_", " ").toLowerCase(Locale.ROOT);

        player.showTitle(Title.title(
                Component.text("Ice: ").color(TextColor.color(0x7F7F7F))
                        .append(Component.text(name).color(TextColor.color(0xC1ADFE))),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
        ));
    }

    private void handleRightClick(Player player, Block clickedBlock, org.bukkit.block.BlockFace face) {
        if (clickedBlock == null || face == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastPlaceTime.containsKey(uuid) && now - lastPlaceTime.get(uuid) < 2000) {
            player.sendActionBar(Component.text("Ice placement is cooling down. Please wait.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Block placeTarget = clickedBlock.getRelative(face);
        if (!placeTarget.getType().isAir() && !placeTarget.isReplaceable()) return;

        Material selected = ICE_TYPES.get(selectedIndex.getOrDefault(uuid, 0));
        placeTarget.setType(selected);
        lastPlaceTime.put(uuid, now);

        player.getWorld().playSound(placeTarget.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, placeTarget.getLocation().add(0.5, 0.5, 0.5), 10,
                new ItemStack(selected));
    }
}
