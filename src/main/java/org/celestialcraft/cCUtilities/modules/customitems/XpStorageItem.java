package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.ArrayList;
import java.util.List;

public class XpStorageItem implements CustomItem {

    private static final String LORE_HEADER = "§7XP Storage";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final TextColor HEX = TextColor.fromHexString("#C1AFDE");
    private static final NamespacedKey STORED_KEY = new NamespacedKey(CCUtilities.getInstance(), "xp_storage_levels");
    private static final int MAX = 500;

    @Override
    public String getIdentifier() {
        return "xp_storage";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(STORED_KEY, PersistentDataType.INTEGER)) return true;
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component c : lore) if (LORE_HEADER.equals(LEGACY.serialize(c))) return true;
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) return;
        var player = event.getPlayer();
        if (!player.isSneaking()) return;

        var item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        int stored = getStored(item);
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (stored >= MAX) {
                player.sendMessage(Component.text("This item can only store up to ").color(NamedTextColor.YELLOW)
                        .append(Component.text(MAX, NamedTextColor.GOLD))
                        .append(Component.text(" levels.", NamedTextColor.YELLOW)));
                event.setCancelled(true);
                return;
            }
            if (player.getLevel() <= 0) {
                player.sendMessage(Component.text("You need at least 1 level to store!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
            player.setLevel(player.getLevel() - 1);
            stored += 1;
            setStoredAndLore(item, stored);
            event.setCancelled(true);
            return;
        }

        if (stored <= 0) {
            player.sendMessage(Component.text("There is no XP stored in this item!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        player.setLevel(player.getLevel() + stored);
        setStoredAndLore(item, 0);
        player.sendMessage(Component.text("You withdrew ", NamedTextColor.GREEN)
                .append(Component.text(stored, NamedTextColor.WHITE))
                .append(Component.text(" XP levels!", NamedTextColor.GREEN)));
        event.setCancelled(true);
    }

    private int getStored(ItemStack item) {
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        Integer val = pdc.get(STORED_KEY, PersistentDataType.INTEGER);
        if (val != null) return val;

        if (meta.hasLore()) {
            List<Component> lore = meta.lore();
            if (lore != null) {
                for (Component c : lore) {
                    String plain = PLAIN.serialize(c).trim();
                    if (plain.startsWith("Stored:")) {
                        String num = plain.replace("Stored:", "").trim();
                        try { return Integer.parseInt(num); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return 0;
    }

    private void setStoredAndLore(ItemStack item, int stored) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(STORED_KEY, PersistentDataType.INTEGER, stored);

        List<Component> old = meta.lore();
        List<Component> nl = new ArrayList<>();
        nl.add(LEGACY.deserialize(LORE_HEADER));
        nl.add(Component.text("Stored: " + stored, HEX));
        if (old != null && old.size() > 2) nl.addAll(old.subList(2, old.size()));

        meta.lore(nl);
        item.setItemMeta(meta);
    }
}
