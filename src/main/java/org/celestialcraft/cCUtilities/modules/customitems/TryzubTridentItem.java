package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class TryzubTridentItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "tryzub_trident";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if ("§7Tryzub Trident".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isValid() || player.isDead()) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) {
            removeEffect(player, PotionEffectType.SPEED);
            removeEffect(player, PotionEffectType.JUMP_BOOST);
            return;
        }

        boolean inWater = player.isInWater();
        boolean rainingOnPlayer = player.getWorld().hasStorm()
                && player.getWorld().getHighestBlockAt(player.getLocation()).getY() <= player.getLocation().getY();

        if (inWater || rainingOnPlayer) {
            applyEffect(player, PotionEffectType.SPEED);
            applyEffect(player, PotionEffectType.JUMP_BOOST);
        } else {
            removeEffect(player, PotionEffectType.SPEED);
            removeEffect(player, PotionEffectType.JUMP_BOOST);
        }
    }

    private void applyEffect(Player player, PotionEffectType type) {
        int amplifier = 1;
        PotionEffect existing = player.getPotionEffect(type);
        if (existing == null || existing.getAmplifier() != amplifier || existing.getDuration() < 40) {
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, false));
        }
    }

    private void removeEffect(Player player, PotionEffectType type) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && existing.getAmplifier() == 1 && existing.getDuration() <= 60) {
            player.removePotionEffect(type);
        }
    }
}
