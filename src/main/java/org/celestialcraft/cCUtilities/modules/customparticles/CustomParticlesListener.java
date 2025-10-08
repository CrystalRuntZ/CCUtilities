package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.EnchantUtil;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class CustomParticlesListener implements Listener {

    private static final boolean DEBUG = false;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    // PDC identifiers (stable), with lore fallback
    private static final Map<String, ParticleEffectType> IDENT_TO_EFFECT = Map.ofEntries(
            Map.entry("flame_ring_particles",      ParticleEffectType.FLAME_RING),
            Map.entry("water_splash_particles",    ParticleEffectType.WATER_SPLASH),
            Map.entry("dragon_fire_particles",     ParticleEffectType.DRAGON_FIRE),
            Map.entry("heart_trail_particles",     ParticleEffectType.HEART_TRAIL),
            Map.entry("lightning_arc_particles",   ParticleEffectType.LIGHTNING_ARC),
            Map.entry("star_trail_particles",      ParticleEffectType.STAR_TRAIL),
            Map.entry("rainbow_spiral_particles",  ParticleEffectType.RAINBOW_SPIRAL),
            Map.entry("wing_burst_particles",      ParticleEffectType.WING_BURST),
            Map.entry("cherry_wing_particles",     ParticleEffectType.CHERRY_WINGS),
            Map.entry("cloud_aura_particles",      ParticleEffectType.CLOUD_AURA),
            Map.entry("autumn_particles",          ParticleEffectType.AUTUMN_LEAVES),
            Map.entry("bloody_icosphere_particles", ParticleEffectType.BLOODY_ICOSPHERE)
    );

    private static final Map<String, ParticleEffectType> LORE_TO_EFFECT = Map.ofEntries(
            Map.entry("&7Flame Ring",                 ParticleEffectType.FLAME_RING),
            Map.entry("&7Flame Ring Particles",       ParticleEffectType.FLAME_RING),

            Map.entry("&7Water Splash",               ParticleEffectType.WATER_SPLASH),
            Map.entry("&7Water Splash Particles",     ParticleEffectType.WATER_SPLASH),

            Map.entry("&7Dragon Fire",                ParticleEffectType.DRAGON_FIRE),
            Map.entry("&7Dragon Fire Particles",      ParticleEffectType.DRAGON_FIRE),

            Map.entry("&7Heart Trail",                ParticleEffectType.HEART_TRAIL),
            Map.entry("&7Heart Trail Particles",      ParticleEffectType.HEART_TRAIL),

            Map.entry("&7Lightning Arc",              ParticleEffectType.LIGHTNING_ARC),
            Map.entry("&7Lightning Arc Particles",    ParticleEffectType.LIGHTNING_ARC),

            Map.entry("&7Star Trail",                 ParticleEffectType.STAR_TRAIL),
            Map.entry("&7Star Trail Particles",       ParticleEffectType.STAR_TRAIL),

            Map.entry("&7Rainbow Spiral",             ParticleEffectType.RAINBOW_SPIRAL),
            Map.entry("&7Rainbow Spiral Particles",   ParticleEffectType.RAINBOW_SPIRAL),

            Map.entry("&7Wing Burst",                 ParticleEffectType.WING_BURST),
            Map.entry("&7Wing Burst Particles",       ParticleEffectType.WING_BURST),

            Map.entry("&7Cherry Wings",               ParticleEffectType.CHERRY_WINGS),
            Map.entry("&7Cherry Wing Particles",      ParticleEffectType.CHERRY_WINGS),

            Map.entry("&7Cloud Aura",                 ParticleEffectType.CLOUD_AURA),
            Map.entry("&7Cloud Aura Particles",       ParticleEffectType.CLOUD_AURA),

            Map.entry("&7Autumn Particles",           ParticleEffectType.AUTUMN_LEAVES),

            Map.entry("&7Bloody Icosphere Particles", ParticleEffectType.BLOODY_ICOSPHERE)
    );

    // ---- events that can change equipment/hand state ----

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { ParticleManager.clearPlayer(e.getPlayer()); ParticleActiveCache.clear(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent e) { refresh(e.getPlayer()); }

    @EventHandler public void onHeld(PlayerItemHeldEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onPickup(PlayerAttemptPickupItemEvent e) { refresh(e.getPlayer()); }
    @EventHandler public void onInvClose(InventoryCloseEvent e) { if (e.getPlayer() instanceof Player p) refreshLater(p); }

    @EventHandler public void onInvClick(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) refreshLater(p); }
    @EventHandler public void onInvDrag(InventoryDragEvent e)   { if (e.getWhoClicked() instanceof Player p) refreshLater(p); }

    private void refreshLater(Player p) {
        Bukkit.getScheduler().runTask(CCUtilities.getInstance(), () -> refresh(p));
    }

    // ---- refresh particles efficiently using ParticleActiveCache ----
    public void refresh(Player p) {
        if (p == null) return;

        // Check if player has any particle effects active
        boolean hasAny = IDENT_TO_EFFECT.keySet().stream().anyMatch(id -> hasAnywherePdc(p, id))
                || LORE_TO_EFFECT.keySet().stream().anyMatch(lore -> hasAnywhereLoreRobust(p, lore));

        // Skip if cached state matches
        if (ParticleActiveCache.isActive(p) == hasAny) return;

        EnumSet<ParticleEffectType> effects = EnumSet.noneOf(ParticleEffectType.class);

        IDENT_TO_EFFECT.forEach((id, effect) -> {
            if (hasAnywherePdc(p, id)) effects.add(effect);
        });

        LORE_TO_EFFECT.forEach((raw, effect) -> {
            if (hasAnywhereLoreRobust(p, raw)) effects.add(effect);
        });

        if (DEBUG) {
            Bukkit.getLogger().info("[CustomParticles] Refresh for " + p.getName() + " -> " + effects);
        }

        ParticleManager.clearPlayer(p);
        effects.forEach(effect -> ParticleManager.register(p, effect));

        // Update the active cache
        ParticleActiveCache.update(p, item ->
                IDENT_TO_EFFECT.keySet().stream().anyMatch(id -> EnchantUtil.hasTag(item, id))
                        || LORE_TO_EFFECT.keySet().stream().anyMatch(lore -> LoreUtil.itemHasLore(item, lore))
        );
    }

    // ---------- Detection helpers ----------
    private boolean hasAnywherePdc(Player p, String id) {
        return hasTag(p.getInventory().getItemInMainHand(), id)
                || hasTag(p.getInventory().getItemInOffHand(), id)
                || Arrays.stream(p.getInventory().getArmorContents()).anyMatch(it -> hasTag(it, id));
    }

    private boolean hasTag(ItemStack item, String id) {
        try { return EnchantUtil.hasTag(item, id); }
        catch (Throwable ignored) { return false; }
    }

    private boolean hasAnywhereLoreRobust(Player p, String targetRawLore) {
        if (matchesLore(p.getInventory().getItemInMainHand(), targetRawLore)) return true;
        if (matchesLore(p.getInventory().getItemInOffHand(), targetRawLore)) return true;
        return Arrays.stream(p.getInventory().getArmorContents()).anyMatch(it -> matchesLore(it, targetRawLore));
    }

    private boolean matchesLore(ItemStack item, String targetRawLore) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return LoreUtil.itemHasLore(item, targetRawLore);
        }

        String target = stripColors(normalizeCodes(targetRawLore));
        for (Component line : lore) {
            String legacy = LEGACY.serialize(line);
            String stripped = stripColors(legacy);
            if (stripped.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    private static String normalizeCodes(String s) {
        return s == null ? "" : s.replace('&', '§');
    }

    private static String stripColors(String legacyColored) {
        return legacyColored == null ? "" : ChatColor.stripColor(legacyColored).trim();
    }
}
