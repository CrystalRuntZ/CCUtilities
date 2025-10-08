package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TrickOrTreatItem implements CustomItem {

    private static final String L1_RAW = "§7Trick or Treat";
    private static final String L2_UNUSED_RAW = "&8Unused";
    private static final String L2_USED_RAW   = "&8Used";

    private static final String L1 = L1_RAW.replace('&','§');
    private static final String L2_UNUSED = L2_UNUSED_RAW.replace('&','§');
    private static final String L2_USED   = L2_USED_RAW.replace('&','§');

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public String getIdentifier() { return "trick_or_treat"; }
    public String getLoreLine()   { return L1_RAW; }
    public boolean appliesTo(ItemStack item) { return item != null; }
    public boolean matches(ItemStack item) { return appliesTo(item) && hasPrimaryLore(item); }

    private static boolean isRightClick(Action a) {
        return a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
    }

    private static boolean hasPrimaryLore(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        String s0 = LEGACY.serialize(lore.getFirst());
        return L1.equalsIgnoreCase(s0);
    }

    private static boolean isUnused(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        var lore = meta != null ? meta.lore() : null;
        if (lore == null || lore.size() < 2) return false;
        String s1 = LEGACY.serialize(lore.get(1));
        return L2_UNUSED.equalsIgnoreCase(s1);
    }

    private static void markUsed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;

        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>(lore);
        if (newLore.size() == 1) {
            newLore.add(LEGACY.deserialize(L2_USED));
        } else {
            newLore.set(1, LEGACY.deserialize(L2_USED));
        }
        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    // Only reward numbers 1..8 possible
    private static int randomReward() {
        return ThreadLocalRandom.current().nextInt(1, 9); // 1..8 inclusive
    }

    private static int rewardAmount(int rewardNumber) {
        return switch (rewardNumber) {
            case 4 -> 64;
            case 5 -> 32;
            case 8 -> 8;
            default -> 1;
        };
    }

    @Override
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!isRightClick(e.getAction())) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (!hasPrimaryLore(inHand)) return;
        if (!isUnused(inHand)) {
            p.sendActionBar(Component.text("🎃 Already used."));
            return;
        }

        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.remove(p);
        if (candidates.isEmpty()) {
            p.sendActionBar(Component.text("👻 No other players online."));
            return;
        }
        Player other = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        int a = randomReward();
        int b = randomReward();
        int amountA = rewardAmount(a);
        int amountB = rewardAmount(b);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "si give tot" + a + " " + amountA + " " + p.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "si give tot" + b + " " + amountB + " " + other.getName());

        markUsed(inHand);

        p.sendActionBar(Component.text("🍬 You and " + other.getName() + " received treats!"));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        // Optional if your interface requires
    }
}
