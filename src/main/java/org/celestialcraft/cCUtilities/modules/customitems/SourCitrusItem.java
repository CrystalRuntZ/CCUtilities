package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class SourCitrusItem implements CustomItem {

    private static final String LORE_LINE = "§7Sour Citrus";
    private static final long COOLDOWN_MS = 3600_000L;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final NamespacedKey lastUseKey = new NamespacedKey(CCUtilities.getInstance(), "sour_citrus_last_use");

    @Override
    public String getIdentifier() {
        return "sour_citrus";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component c : lore) if (LORE_LINE.equals(serializer.serialize(c))) return true;
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        var player = event.getPlayer();
        if (!player.isSneaking()) return;
        var held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        var pdc = player.getPersistentDataContainer();
        long now = System.currentTimeMillis();
        Long last = pdc.get(lastUseKey, PersistentDataType.LONG);
        if (last != null && now - last < COOLDOWN_MS) {
            long remainingMillis = COOLDOWN_MS - (now - last);
            long secondsLeft = (remainingMillis + 999) / 1000; // round up
            player.sendActionBar(Component.text("§cYou must wait " + secondsLeft + "s before using this again."));
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30 * 20, 4, true, true, true));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "feed " + player.getName());
        pdc.set(lastUseKey, PersistentDataType.LONG, now);
        event.setCancelled(true);
    }
}
