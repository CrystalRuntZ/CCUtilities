package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DemonTridentItem implements CustomItem {

    private static final String LORE = "§7Demon's Trident";

    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, UUID> ownerByProjectile = new HashMap<>();
    private final Map<UUID, ItemStack> thrownTridentItems = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "demons_trident";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.TRIDENT) return false;
        return LoreUtil.itemHasLore(item, LORE);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) { /* no-op */ }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) { /* no-op */ }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent ev) {
        Projectile proj = ev.getEntity();
        if (proj.getType() != EntityType.TRIDENT) return;
        if (!(proj.getShooter() instanceof Player shooter)) return;

        ItemStack main = shooter.getInventory().getItemInMainHand();
        ItemStack off = shooter.getInventory().getItemInOffHand();
        ItemStack usedTrident = matches(main) ? main : matches(off) ? off : null;
        if (usedTrident == null) return;

        thrownTridentItems.put(proj.getUniqueId(), usedTrident.clone());

        Location playAt = shooter.getLocation();
        if (playAt.getWorld() != null) {
            playAt.getWorld().playSound(playAt, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        UUID projId = proj.getUniqueId();
        UUID ownerId = shooter.getUniqueId();
        ownerByProjectile.put(projId, ownerId);

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            private boolean hasReturned = false;

            @Override
            public void run() {
                Entity current = Bukkit.getEntity(projId);
                Player owner = Bukkit.getPlayer(ownerId);

                if (owner == null) {
                    if (current != null && !current.isDead()) {
                        Location dropLoc = current.getLocation();
                        current.remove();
                        giveExactTridentToOwner(null, projId, dropLoc); // drops at location
                    }
                    cancelTaskFor(projId);
                    return;
                }

                if (current == null || current.isDead()) {
                    if (!hasReturned) {
                        giveExactTridentToOwner(owner, projId, owner.getLocation());
                        hasReturned = true;
                    }
                    cancelTaskFor(projId);
                    return;
                }

                if (current instanceof Item dropped) {
                    ItemStack stack = dropped.getItemStack();
                    if (stack.getType() == Material.TRIDENT) {
                        Location spawnLoc = owner.getLocation();
                        current.remove();
                        if (!hasReturned) {
                            giveExactTridentToOwner(owner, projId, spawnLoc);
                            hasReturned = true;
                        }
                        cancelTaskFor(projId);
                        return;
                    }
                }

                if (current instanceof org.bukkit.entity.Trident trident) {
                    if (trident.hasDealtDamage() && trident.getVelocity().lengthSquared() < 0.02) {
                        Location spawnLoc = owner.getLocation();
                        trident.remove();
                        if (!hasReturned) {
                            giveExactTridentToOwner(owner, projId, spawnLoc);
                            hasReturned = true;
                        }
                        cancelTaskFor(projId);
                        return;
                    }
                    if (trident.getLocation().getY() < -64.0) {
                        trident.remove();
                        if (!hasReturned) {
                            giveExactTridentToOwner(owner, projId, owner.getLocation());
                            hasReturned = true;
                        }
                        cancelTaskFor(projId);
                    }
                }
            }
        }.runTaskTimer(CCUtilities.getInstance(), 1L, 1L);

        activeTasks.put(projId, task);
    }

    @Override
    public void onQuit(PlayerQuitEvent ev) {
        UUID owner = ev.getPlayer().getUniqueId();
        ownerByProjectile.entrySet().removeIf(entry -> {
            UUID projId = entry.getKey();
            UUID o = entry.getValue();
            if (o.equals(owner)) {
                cancelTaskFor(projId);
                return true;
            }
            return false;
        });
    }

    private void cancelTaskFor(UUID projId) {
        BukkitTask t = activeTasks.remove(projId);
        if (t != null) t.cancel();
        ownerByProjectile.remove(projId);
        thrownTridentItems.remove(projId);
    }

    private void giveExactTridentToOwner(Player owner, UUID projId, Location atLocation) {
        ItemStack trident = thrownTridentItems.remove(projId);
        if (trident == null) trident = new ItemStack(Material.TRIDENT, 1); // fallback

        if (owner == null || !owner.isOnline()) {
            if (atLocation != null && atLocation.getWorld() != null) {
                atLocation.getWorld().dropItemNaturally(atLocation, trident);
            }
            return;
        }

        org.bukkit.inventory.PlayerInventory inv = owner.getInventory();
        ItemStack mainHand = inv.getItemInMainHand();
        if (mainHand.getType() == Material.AIR) {
            inv.setItemInMainHand(trident);
            return;
        }
        Map<Integer, ItemStack> leftover = inv.addItem(trident);
        if (!leftover.isEmpty() && atLocation != null && atLocation.getWorld() != null) {
            atLocation.getWorld().dropItemNaturally(atLocation, trident);
        }
    }
}