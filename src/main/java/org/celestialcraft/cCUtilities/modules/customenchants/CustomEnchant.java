package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public interface CustomEnchant {
    String getIdentifier();
    boolean appliesTo(ItemStack item);
    boolean hasEnchant(ItemStack item);
    void applyEffect(EntityDamageByEntityEvent event);
    ItemStack applyTo(ItemStack item);
    String getLoreLine();

    default void onJoin(PlayerJoinEvent event) {}
    default void onQuit(PlayerQuitEvent event) {}
    default void onHeld(PlayerItemHeldEvent event) {}
    default void onHandSwap(Player player) {}
    default void onShootBow(EntityShootBowEvent event) {}
    default void onPlayerMove(Player player) {}
    default void onEntityTarget(EntityTargetLivingEntityEvent event) {}
    default void onTick(Player player, ItemStack item) {}
    default void onMove(PlayerMoveEvent event) {}
    default void onArmorEquip(Player player) {}
    default void onArmorUnequip(Player player) {}
}
