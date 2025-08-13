package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomEnchantEffectListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.applyEffect(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] applyEffect failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onEntityTarget(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onEntityTarget failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onShootBow(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onShootBow failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onPlayerDeath(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onPlayerDeath failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onJoin(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onJoin failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onQuit(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onQuit failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onHeld(event);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onHeld failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onPlayerMove(player);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onPlayerMove failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        Player player = event.getPlayer();
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            try {
                enchant.onHandSwap(player);
            } catch (Throwable t) {
                CCUtilities.getInstance().getLogger().warning("[CustomEnchants] onHandSwap failed for " + enchant.getIdentifier() + ": " + t.getMessage());
            }
        }
    }
}
