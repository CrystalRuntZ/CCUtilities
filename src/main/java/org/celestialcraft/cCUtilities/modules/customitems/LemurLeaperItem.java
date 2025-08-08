package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class LemurLeaperItem implements CustomItem {
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "lemur_leaper";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;

        var meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if ("§7Lemur Leaper's".equals(legacy.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAny(Player player) {
        List<ItemStack> allItems = new ArrayList<>();
        allItems.add(player.getInventory().getItemInMainHand());
        allItems.add(player.getInventory().getItemInOffHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                allItems.add(armor);
            }
        }
        for (ItemStack item : allItems) {
            if (matches(item)) return true;
        }
        return false;
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isValid() || player.isDead()) return;

        if (!hasAny(player)) {
            removeEffect(player, PotionEffectType.JUMP_BOOST);
            removeEffect(player, PotionEffectType.SLOW_FALLING);
            return;
        }

        applyEffect(player, PotionEffectType.JUMP_BOOST, 2);
        applyEffect(player, PotionEffectType.SLOW_FALLING, 0);
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing == null || existing.getAmplifier() != amplifier || existing.getDuration() < 40) {
            player.addPotionEffect(new PotionEffect(type, 60, amplifier, true, false, false));
        }
    }

    private void removeEffect(Player player, PotionEffectType type) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && existing.getAmplifier() <= 2 && existing.getDuration() <= 60) {
            player.removePotionEffect(type);
        }
    }
}
