package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class BowOfLevitationItem implements CustomItem {

    private static final String LORE_LINE = "§7Bow of Levitation";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final NamespacedKey KEY = new NamespacedKey(CCUtilities.getInstance(), "levibow_owner");

    @Override
    public String getIdentifier() {
        return "bow_of_levitation";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (!matches(bow)) return;

        arrow.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, shooter.getUniqueId().toString());
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        PersistentDataContainer container = arrow.getPersistentDataContainer();
        if (!container.has(KEY, PersistentDataType.STRING)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 200, 2)); // 10 seconds, level 3
    }
}
