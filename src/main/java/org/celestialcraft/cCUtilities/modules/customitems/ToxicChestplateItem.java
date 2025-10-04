package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.List;

public class ToxicChestplateItem implements CustomItem {

    private static final String RAW_LORE = "&7Toxic Chestplate";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    public static final NamespacedKey TOXIC_CHESTPLATE_KEY = new NamespacedKey(CCUtilities.getInstance(), "toxic_chestplate");


    @Override
    public String getIdentifier() {
        return "toxic_chestplate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;

        boolean loreMatch = lore.stream().anyMatch(line -> line.equals(LORE_LINE));
        boolean tagMatch = meta.getPersistentDataContainer().has(TOXIC_CHESTPLATE_KEY, PersistentDataType.BYTE);

        return loreMatch || tagMatch;
    }

    @Override
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack chestplate = victim.getInventory().getChestplate();
        if (!matches(chestplate)) return;

        if (!ClaimUtils.canBuild(victim, victim.getLocation())) return;

        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity attacker) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
        }
    }

    @Override
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        // Not used
    }
}
