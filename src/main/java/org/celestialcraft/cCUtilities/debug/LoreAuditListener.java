package org.celestialcraft.cCUtilities.debug;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class LoreAuditListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static List<String> loreLines(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.emptyList();
        ItemMeta meta = item.getItemMeta();
        List<String> out = new ArrayList<>();
        if (meta.lore() != null) {
            for (var c : Objects.requireNonNull(meta.lore())) {
                if (c == null) continue;
                String s = LEGACY.serialize(c).replace("§o", ""); // ignore italics for stable compare
                out.add(s);
            }
        } else if (meta.hasLore()) {
            @SuppressWarnings("deprecation")
            List<String> legacy = meta.getLore();
            if (legacy != null) {
                for (String s : legacy) out.add(s == null ? "" : s.replace("§o",""));
            }
        }
        return out;
    }

    private static class Snapshot {
        final Map<Integer, List<String>> slotLore = new HashMap<>();
    }

    private static Snapshot snapshot(Player p) {
        Snapshot snap = new Snapshot();
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            snap.slotLore.put(i, loreLines(contents[i]));
        }
        return snap;
    }

    private static void compareAndLog(Player p, String cause, Snapshot before) {
        PlayerInventory inv = p.getInventory();
        boolean any = false;
        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack now = inv.getItem(i);
            List<String> pre = before.slotLore.getOrDefault(i, Collections.emptyList());
            List<String> post = loreLines(now);
            if (!Objects.equals(pre, post)) {
                any = true;
                CCUtilities.getInstance().getLogger().warning(
                        "[LoreAudit] " + p.getName() + " slot " + i +
                                " (" + (now == null ? Material.AIR : now.getType()) + ") changed via " + cause +
                                " | before=" + pre + " | after=" + post
                );
            }
        }
        if (any) CCUtilities.getInstance().getLogger().warning("--------------------------------------------------");
    }

    private void auditSoon(Player p, String cause) {
        if (p == null) return;
        Snapshot before = snapshot(p);
        Bukkit.getScheduler().runTask(CCUtilities.getInstance(), () -> compareAndLog(p, cause, before));
    }

    // ======================
    // Events (no redundant defaults)
    // ======================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvil(PrepareAnvilEvent e) {
        if (e.getView().getPlayer() instanceof Player p) auditSoon(p, "PrepareAnvilEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvilClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) auditSoon(p, "InventoryClickEvent(" + e.getSlotType() + ")");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeld(PlayerItemHeldEvent e) { auditSoon(e.getPlayer(), "PlayerItemHeldEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent e) { auditSoon(e.getPlayer(), "PlayerSwapHandItemsEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        e.getTo();
        if (e.getFrom().distanceSquared(e.getTo()) < 1.0e-4) return;
        auditSoon(e.getPlayer(), "PlayerMoveEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) { auditSoon(e.getPlayer(), "BlockBreakEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemDamage(PlayerItemDamageEvent e) { auditSoon(e.getPlayer(), "PlayerItemDamageEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) { auditSoon(e.getPlayer(), "PlayerInteractEvent(" + e.getAction() + ")"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) { auditSoon(e.getPlayer(), "PlayerJoinEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) { auditSoon(e.getPlayer(), "PlayerRespawnEvent"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Bukkit.getScheduler().runTask(CCUtilities.getInstance(), () -> {
            Player alive = Bukkit.getPlayer(p.getUniqueId());
            if (alive != null) auditSoon(alive, "PlayerDeathEvent(post)");
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShoot(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p) auditSoon(p, "EntityShootBowEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player p) auditSoon(p, "EntityTargetLivingEntityEvent[target]");
        if (e.getEntity() instanceof Player p2) auditSoon(p2, "EntityTargetLivingEntityEvent[entity]");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player p) auditSoon(p, "EntityDamageByEntityEvent[victim]");
        if (e.getDamager() instanceof Player p2) auditSoon(p2, "EntityDamageByEntityEvent[damager]");
    }
}
