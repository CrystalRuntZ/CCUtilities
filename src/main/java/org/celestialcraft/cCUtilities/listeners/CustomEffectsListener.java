package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.modules.customitems.CustomItem;
import org.celestialcraft.cCUtilities.modules.customitems.CustomItemRegistry;
import org.celestialcraft.cCUtilities.modules.customitems.SpiderBackpackItem;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomEffectsListener implements Listener {

    private final SpiderBackpackItem spiderBackpackItem;

    public CustomEffectsListener(SpiderBackpackItem spiderBackpackItem) {
        this.spiderBackpackItem = spiderBackpackItem;
    }

    // --- Custom Items ---

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        spiderBackpackItem.handleUse(event);

        ItemStack item = event.getItem();
        CustomItem custom = CustomItemRegistry.get(item);

        if (custom instanceof org.celestialcraft.cCUtilities.modules.customitems.BlackCatSpawnEggItem blackCatEggItem) {
            blackCatEggItem.onInteract(event);
        }

        if (custom != null) {
            custom.onRightClick(event);
            custom.onInteract(event);
            if (event.getPlayer().isSneaking()) {
                custom.onRightClickSneak(event);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        CustomItem custom = CustomItemRegistry.get(item);
        if (custom != null) {
            custom.onLeftClick(event);
            custom.onInteract(event);
            if (event.getPlayer().isSneaking()) {
                custom.onLeftClickSneak(event);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onEntityTarget(event);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem custom = CustomItemRegistry.get(item);
        if (custom != null) {
            custom.onInteractEntity(event);
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onBedEnter(event);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onWorldChange(event);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onProjectileHit(event);
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        ItemStack item = event.getItem();
        CustomItem custom = CustomItemRegistry.get(item);
        if (custom != null) {
            custom.onConsume(event.getPlayer(), item);
            custom.onConsume(event);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        CustomItem custom = CustomItemRegistry.get(item);
        if (custom != null) {
            custom.onFish(event);
        }
    }

    @EventHandler
    public void onArmorChange(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            CustomItem oldItem = CustomItemRegistry.get(current);
            if (oldItem != null) oldItem.onArmorUnequip(player);
        }

        ItemStack newItem = event.getCursor();
        if (!newItem.getType().isAir()) {
            CustomItem newCustom = CustomItemRegistry.get(newItem);
            if (newCustom != null) newCustom.onArmorEquip(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;

        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            CustomItem custom = CustomItemRegistry.get(item);
            if (custom != null) {
                custom.onAttack(event);
                custom.onEntityDamage(event);
            }
        }

        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onCombat(event);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;

        if (event.getEntity().getShooter() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            CustomItem custom = CustomItemRegistry.get(item);
            if (custom != null) custom.onProjectileLaunch(event);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem custom = CustomItemRegistry.get(item);
        if (custom != null) {
            custom.onBlockBreak(player, event.getBlock(), item, event);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        for (CustomItem custom : CustomItemRegistry.getAll()) {
            custom.onTeleport(player, event);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        if (event.getEntity() instanceof Player player) {
            for (CustomItem item : CustomItemRegistry.getAll()) {
                item.onFallDamage(player, event);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem custom : CustomItemRegistry.getAll()) {
            custom.onMove(event);
        }
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onToggleGlide(event);
        }
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem custom : CustomItemRegistry.getAll()) {
            custom.onResurrect(event);
        }
    }

    @EventHandler
    public void onArmorChangePaper(com.destroystokyo.paper.event.player.PlayerArmorChangeEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onArmorChange(player, newItem);
        }
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        Player player = event.getPlayer();
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onItemHeld(player);
            item.onHeld(event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;

        if (event.getEntity() instanceof Cat cat) {
            for (CustomItem item : CustomItemRegistry.getAll()) {
                if (item instanceof org.celestialcraft.cCUtilities.modules.customitems.BlackCatSpawnEggItem blackCatEgg) {
                    blackCatEgg.onCatDeath(cat);
                }
                item.onEntityDeath(event);
            }
        } else {
            for (CustomItem item : CustomItemRegistry.getAll()) {
                item.onEntityDeath(event);
            }
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onHandSwap(event);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onEntityDamageByEntity(event);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onInventoryClick(event);
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            CustomItem oldItem = CustomItemRegistry.get(current);
            if (oldItem != null) oldItem.onArmorUnequip(player);
        }

        ItemStack newItem = event.getCursor();
        if (!newItem.getType().isAir()) {
            CustomItem newCustom = CustomItemRegistry.get(newItem);
            if (newCustom != null) newCustom.onArmorEquip(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        spiderBackpackItem.handleInventoryClose(event);
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onInventoryClose(event);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onJoin(event);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!ModuleManager.isEnabled("customitems")) return;
        spiderBackpackItem.handleQuit(event);
        for (CustomItem item : CustomItemRegistry.getAll()) {
            item.onQuit(event);
        }
    }
}
