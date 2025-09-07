package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.UUID;

public class BeheadingEnchant implements CustomEnchant {

    private static final String RAW_LORE  = "&7Beheading";

    @Override public String getIdentifier() { return "beheading"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null) return false;
        String n = item.getType().name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE");
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!appliesTo(weapon) || !hasEnchant(weapon)) return;

        // If a player head is already going to drop, don’t add another
        if (dropsAlreadyContainVictimHead(event, victim.getUniqueId())) return;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(victim.getUniqueId()));
            skullMeta.displayName(Component.text(victim.getName() + "'s Head"));
            head.setItemMeta(skullMeta);
        }
        event.getDrops().add(head);
    }

    private boolean dropsAlreadyContainVictimHead(PlayerDeathEvent event, UUID victimId) {
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() != Material.PLAYER_HEAD) continue;
            ItemMeta im = drop.getItemMeta();
            if (im instanceof SkullMeta sm && sm.getOwningPlayer() != null
                    && victimId.equals(sm.getOwningPlayer().getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
