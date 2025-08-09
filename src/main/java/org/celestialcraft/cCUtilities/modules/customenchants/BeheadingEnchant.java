package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class BeheadingEnchant implements CustomEnchant {

    private static final String LORE_LINE = "§7Beheading";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "beheading";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.getType().isItem() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return appliesTo(item);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (!item.getType().isItem()) return item;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) lore = new java.util.ArrayList<>();
        lore.add(LORE_COMPONENT);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // Not used
    }

    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!hasEnchant(weapon)) return;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(victim.getUniqueId()));
            skullMeta.displayName(Component.text(victim.getName() + "'s Head"));
            head.setItemMeta(skullMeta);
        }

        victim.getWorld().dropItemNaturally(victim.getLocation(), head);
    }

    @Override
    public String getLoreLine() {
        return "&7Beheading";
    }
}
