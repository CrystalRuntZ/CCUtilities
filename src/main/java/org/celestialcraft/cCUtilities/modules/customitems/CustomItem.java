package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public interface CustomItem {

    String getIdentifier();
    boolean matches(ItemStack item);
    default void onRightClick(PlayerInteractEvent event) {}
    default void onEntityTarget(EntityTargetLivingEntityEvent event) {}
    default void onLeftClick(PlayerInteractEvent event) {}
    default void onRightClickSneak(PlayerInteractEvent event) {}
    default void onInteract(PlayerInteractEvent event) {}
    default void onConsume(Player player, ItemStack item) {}
    default void onConsume(PlayerItemConsumeEvent event) {}
    default void onFish(PlayerFishEvent event) {}
    default void onArmorEquip(Player player) {}
    default void onArmorUnequip(Player player) {}
    default void onArmorChange(Player player, ItemStack newItem) {}
    default void onAttack(EntityDamageByEntityEvent event) {}
    default void onCombat(EntityDamageByEntityEvent event) {}
    default void onEntityDamage(EntityDamageEvent event) {}
    default void onProjectileLaunch(ProjectileLaunchEvent event) {}
    default void onBlockBreak(Player player, org.bukkit.block.Block block, ItemStack tool, BlockBreakEvent event) {}
    default void onTeleport(Player player, PlayerTeleportEvent event) {}
    default void onFallDamage(Player player, EntityDamageEvent event) {}
    default void onMove(PlayerMoveEvent event) {}
    default void onResurrect(EntityResurrectEvent event) {}
    default void onLeftClickSneak(PlayerInteractEvent event) {}
    default void onItemHeld(Player player) {}
    default void onHeld(PlayerItemHeldEvent event) {}
    default void onHandSwap(PlayerSwapHandItemsEvent event) {}
    default void onInventoryClick(InventoryClickEvent event) {}
    default void onInventoryClose(InventoryCloseEvent event) {}
    default void onInteractEntity(PlayerInteractEntityEvent event) {}
    default void onRightClickEntity(PlayerInteractEntityEvent event) {}
    default void onJoin(PlayerJoinEvent event) {}
    default void onQuit(PlayerQuitEvent event) {}
    default void onEntityDamage(EntityDamageByEntityEvent event) {}
    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) {}
    default void onEntityDeath(EntityDeathEvent event) { }
    default void onInteractEntity(PlayerInteractAtEntityEvent event) { }
    default void onProjectileHit(ProjectileHitEvent event) { }
    default void onWorldChange(PlayerChangedWorldEvent event) { }
    default void onBedEnter(PlayerBedEnterEvent event) { }
}
