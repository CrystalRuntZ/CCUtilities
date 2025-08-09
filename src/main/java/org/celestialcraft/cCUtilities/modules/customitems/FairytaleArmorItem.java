package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FairytaleArmorItem implements CustomItem {

    private static final String LORE_LINE = "§7Fairytale Armor";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "fairytale_armor";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    private void applyEffects(Player player) {
        // Remove old effects first
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        // Count armor pieces with matching lore
        int pieces = 0;
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (matches(armorPiece)) {
                pieces++;
            }
        }

        // Apply effects based on piece count
        if (pieces == 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
        } else if (pieces == 3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 0, true, false));
        } else if (pieces == 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false));
        }
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            // Delay 1 tick like Skript
            player.getServer().getScheduler().runTaskLater(org.celestialcraft.cCUtilities.CCUtilities.getInstance(),
                    () -> applyEffects(player), 1L);
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            player.getServer().getScheduler().runTaskLater(org.celestialcraft.cCUtilities.CCUtilities.getInstance(),
                    () -> applyEffects(player), 1L);
        }
    }

    @Override
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(org.celestialcraft.cCUtilities.CCUtilities.getInstance(),
                () -> applyEffects(player), 1L);
    }

    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(org.celestialcraft.cCUtilities.CCUtilities.getInstance(),
                () -> applyEffects(player), 1L);
    }
}
