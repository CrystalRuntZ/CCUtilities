package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VegemiteItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "vegemite";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) return false;
        List<net.kyori.adventure.text.Component> lore = item.getItemMeta().lore();
        assert lore != null;
        for (net.kyori.adventure.text.Component line : lore) {
            String legacyIdentifier = "§7Vegemite";
            if (serializer.serialize(line).equals(legacyIdentifier)) {
                return true;
            }
        }
        return false;
    }

    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(player.getUniqueId());

        long cooldownMillis = 10 * 60 * 1000L;
        if (lastUsed != null && now - lastUsed < cooldownMillis) {
            int secondsLeft = (int) ((cooldownMillis - (now - lastUsed)) / 1000);
            player.sendActionBar(Component.text("Vegemite cooldown: " + secondsLeft + "s remaining", NamedTextColor.RED));
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20 * 30, 0, true, false, true));
        cooldowns.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text("Vegemite activated!", NamedTextColor.GOLD));
    }
}
