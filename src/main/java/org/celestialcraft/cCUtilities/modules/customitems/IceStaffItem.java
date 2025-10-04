package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IceStaffItem implements CustomItem {

    private static final String LORE_LINE = "§7Ice Staff";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, Integer> mode = new HashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 1000;

    @Override
    public String getIdentifier() {
        return "ice_staff";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onLeftClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        int next = (mode.getOrDefault(id, 0) + 1) % 3;
        mode.put(id, next);

        String selectedName = switch (next) {
            case 1 -> "Packed Ice";
            case 2 -> "Blue Ice";
            default -> "Ice";
        };

        player.sendMessage(mm.deserialize("&7Selected: <#C1AFDE>" + selectedName));
        player.sendActionBar(mm.deserialize("<#C1AFDE>Selected ice mode switched."));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = cooldown.getOrDefault(id, 0L);
        if (now - last < COOLDOWN_MS) {
            player.sendActionBar(mm.deserialize("<red>Ice placement is cooling down. Please wait."));
            return;
        }
        cooldown.put(id, now);

        Block base = event.getClickedBlock();
        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            player.sendMessage(mm.deserialize("<red>You can't place ice here."));
            return;
        }

        int m = mode.getOrDefault(id, 0);
        Material toPlace = switch (m) {
            case 1 -> Material.PACKED_ICE;
            case 2 -> Material.BLUE_ICE;
            default -> Material.ICE;
        };

        above.setType(toPlace);

        player.getWorld().playSound(above.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, above.getLocation().add(0.5, 0.5, 0.5), 15,
                toPlace.createBlockData());
    }
}
