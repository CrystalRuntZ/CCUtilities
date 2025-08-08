package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.List;

public class ToxicChestplateItem implements CustomItem {

    private static final String RAW_LORE = "&7Toxic Chestplate";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

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
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
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
