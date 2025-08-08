package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class LifestealSwordItem implements CustomItem {

    private static final String LORE_LINE = "&7Lifesteal Sword";
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "lifesteal_sword";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                serializer.serialize(component).contains(LORE_LINE));
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        AttributeInstance maxHealthAttr = attacker.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHealth = maxHealthAttr.getValue();
        double healAmount = event.getFinalDamage() * 0.2;
        double newHealth = Math.min(attacker.getHealth() + healAmount, maxHealth);

        attacker.setHealth(newHealth);
    }
}
