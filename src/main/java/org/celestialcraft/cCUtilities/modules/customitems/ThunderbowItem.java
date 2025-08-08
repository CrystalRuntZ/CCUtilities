package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class ThunderbowItem implements CustomItem {

    private static final String RAW_LORE = "&7Thunderbow";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    public static final NamespacedKey THUNDERBOW_KEY =
            new NamespacedKey(CCUtilities.getInstance(), "thunderbow");

    @Override
    public String getIdentifier() {
        return "thunderbow";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (!matches(bow)) return;

        Projectile projectile = event.getEntity();
        projectile.getPersistentDataContainer().set(THUNDERBOW_KEY, PersistentDataType.BYTE, (byte) 1);
        projectile.customName(Component.text("Thunderbow Projectile"));
        projectile.setCustomNameVisible(false);
    }

    @Override
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        if (!projectile.getPersistentDataContainer().has(THUNDERBOW_KEY, PersistentDataType.BYTE)) return;

        Entity target = event.getEntity();
        target.getWorld().strikeLightning(target.getLocation());
    }
}
