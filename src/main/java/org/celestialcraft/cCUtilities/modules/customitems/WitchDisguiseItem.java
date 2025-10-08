package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class WitchDisguiseItem implements CustomItem, Listener {

    private static final String RAW_LORE = "§7Witch Disguise";
    private static final String LORE_SECT = RAW_LORE.replace('&', '§');
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Set<UUID> currentlyDisguised = new HashSet<>();
    private final Plugin plugin;

    public WitchDisguiseItem(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public String getIdentifier() {
        return "witch_disguise";
    }

    public String getLoreLine() {
        return RAW_LORE;
    }

    public boolean appliesTo(ItemStack item) {
        return item != null;
    }

    public boolean matches(ItemStack item) {
        return appliesTo(item) && hasLoreLine(item);
    }

    private static boolean hasLoreLine(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        for (var c : lore) {
            if (c == null) continue;
            if (LEGACY.serialize(c).equalsIgnoreCase(LORE_SECT)) return true;
        }
        return false;
    }

    private static boolean hasDisguiseItemEquippedOrHeld(Player p) {
        PlayerInventory inv = p.getInventory();
        if (hasLoreLine(inv.getItemInMainHand()) || hasLoreLine(inv.getItemInOffHand())) return true;
        for (ItemStack armor : inv.getArmorContents()) if (hasLoreLine(armor)) return true;
        return false;
    }

    private void applyDisguise(Player p) {
        if (currentlyDisguised.contains(p.getUniqueId())) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "disguiseplayer " + p.getName() + " witch setTallSelfDisguise VISIBLE");
        currentlyDisguised.add(p.getUniqueId());
        p.sendActionBar(Component.text("🧹 You are now disguised as a Witch."));
    }

    private void removeDisguise(Player p) {
        if (!currentlyDisguised.contains(p.getUniqueId())) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "undisguiseplayer " + p.getName());
        currentlyDisguised.remove(p.getUniqueId());
        p.sendActionBar(Component.text("✨ Your disguise has been removed."));
    }

    private void updateDisguise(Player p) {
        if (hasDisguiseItemEquippedOrHeld(p)) applyDisguise(p);
        else removeDisguise(p);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDisguise(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        currentlyDisguised.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDisguise(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDisguise(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDisguise(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateDisguise(p), 1L);
    }
}