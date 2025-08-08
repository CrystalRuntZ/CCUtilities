package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

public class ZombieRepellantEnchant implements CustomEnchant {

    private static final String LORE_LINE = "§7Zombie Repellant";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "zombie_repellant";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        var lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return false;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {

    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return null;
    }

    @Override
    public String getLoreLine() {
        return "&7Zombie Repellant";
    }

    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (event.getEntityType() != EntityType.ZOMBIE) return;

        for (ItemStack item : player.getEquipment().getArmorContents()) {
            if (appliesTo(item)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (appliesTo(mainHand)) {
            event.setCancelled(true);
        }
    }
}
