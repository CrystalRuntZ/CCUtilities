package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CriticalChestplateItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Critical Chestplate";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "critical_chestplate";
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
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Check critical hit-like conditions
        if (!attacker.isOnGround() &&
                attacker.getFallDistance() > 0 &&
                !attacker.isSprinting() &&
                !attacker.isInsideVehicle() &&
                attacker.getVelocity().getY() < 0) {

            ItemStack chestplate = attacker.getInventory().getChestplate();
            if (!matches(chestplate)) return;

            double original = event.getDamage();
            double boosted = original * 1.2; // +20% damage
            event.setDamage(boosted);
        }
    }
}
