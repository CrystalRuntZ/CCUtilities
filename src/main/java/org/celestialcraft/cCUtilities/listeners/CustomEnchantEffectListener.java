package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomEnchantEffectListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.applyEffect(event);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onEntityTarget(event);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onPlayerDeath(event);
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onJoin(event);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onQuit(event);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onHeld(event);
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onShootBow(event);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        Player player = event.getPlayer();
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onPlayerMove(player);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        Player player = event.getPlayer();
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            enchant.onHandSwap(player);
        }
    }
}
