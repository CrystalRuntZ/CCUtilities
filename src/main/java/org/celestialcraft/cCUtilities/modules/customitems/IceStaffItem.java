package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

        UUID id = event.getPlayer().getUniqueId();
        int next = (mode.getOrDefault(id, 0) + 1) % 3;
        mode.put(id, next);

        switch (next) {
            case 0 -> event.getPlayer().sendMessage(mm.deserialize("&7Selected: <#C1AFDE>Ice"));
            case 1 -> event.getPlayer().sendMessage(mm.deserialize("&7Selected: <#C1AFDE>Packed Ice"));
            case 2 -> event.getPlayer().sendMessage(mm.deserialize("&7Selected: <#C1AFDE>Blue Ice"));
        }
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        UUID id = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        long last = cooldown.getOrDefault(id, 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldown.put(id, now);

        Block base = event.getClickedBlock();
        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            event.getPlayer().sendMessage(mm.deserialize("<red>You can't place ice here."));
            return;
        }

        int m = mode.getOrDefault(id, 0);
        Material toPlace = switch (m) {
            case 1 -> Material.PACKED_ICE;
            case 2 -> Material.BLUE_ICE;
            default -> Material.ICE;
        };

        above.setType(toPlace);
    }
}
