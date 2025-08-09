package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class DoubleXpSwordItem implements CustomItem {

    private static final String LORE_LINE = "§7Double XP Sword";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "double_xp_sword";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.getKiller() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!matches(weapon)) return;

        Location loc = victim.getLocation();
        for (int i = 0; i < 10; i++) {
            victim.getWorld().spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(1));
        }
    }
}
