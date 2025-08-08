package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GwynnbleiddItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Gwynnbleidd";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "gwynnbleidd";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
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
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!matches(weapon)) return;

        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = attr != null ? attr.getValue() : 20.0;
        double healthRatio = player.getHealth() / maxHealth;
        if (healthRatio <= 0.25) {
            event.setDamage(event.getDamage() * 1.5);
        }

        Entity target = event.getEntity();
        if (target instanceof Wither || target instanceof EnderDragon) {
            event.setDamage(event.getDamage() * 1.1);
        }
    }
}
