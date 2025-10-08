package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.time.Duration;
import java.util.*;

public class MapArtClearingWandListener implements Listener {
    private static final String WORLD_NAME = "maparts";

    // Your canonical lore id (what you write onto the item when creating the wand)
    private static final String LORE_ID = "&7Mapart Clearing Wand";
    private static final String DISPLAY_TEXT = "Mapart Clearing Wand"; // plain text variant for fallback

    private final MapArtDataManager data;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final LegacyComponentSerializer ampersand = LegacyComponentSerializer.legacyAmpersand();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    private final Map<UUID, Material> selection = new HashMap<>();
    private final Map<UUID, Long> rcCd = new HashMap<>();
    private static final long CLEAR_COOLDOWN_MS = 350;

    public MapArtClearingWandListener(MapArtDataManager data) {
        this.data = data;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        var p = e.getPlayer();
        var item = e.getItem();
        if (!matchesWand(item)) return;

        // Gate the feature to the correct world for BOTH left and right clicks
        World w = p.getWorld();
        if (!w.getName().equalsIgnoreCase(WORLD_NAME)) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null) {
            var block = e.getClickedBlock();
            var region = data.regionAt(block.getLocation());
            if (region == null || !data.isClaimed(region.getName())) {
                p.sendMessage(mini.deserialize("<red>Use this wand on your claimed mapart.</red>"));
                e.setCancelled(true);
                return;
            }
            if (!data.isOwner(region.getName(), p.getUniqueId()) && !data.isTrusted(region.getName(), p.getUniqueId())) {
                p.sendMessage(mini.deserialize("<red>You must be the owner or trusted on this mapart.</red>"));
                e.setCancelled(true);
                return;
            }
            Material m = block.getType();
            if (m.isAir()) {
                p.sendMessage(mini.deserialize("<yellow>You cannot select air.</yellow>"));
                e.setCancelled(true);
                return;
            }
            if (isProtectedBlock(block)) {
                p.sendMessage(mini.deserialize("<red>You cannot select that block type with this wand.</red>"));
                e.setCancelled(true);
                return;
            }
            selection.put(p.getUniqueId(), m);
            Component title = mini.deserialize("<gray>Block Selected:</gray> <#c1adfe>" + m.name() + "</#c1adfe>");
            p.showTitle(Title.title(title, Component.empty(), Title.Times.times(
                    Duration.ofMillis(150), Duration.ofMillis(1400), Duration.ofMillis(250))));
            e.setCancelled(true);
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            var region = data.regionAt(p.getLocation());
            if (region == null || !data.isClaimed(region.getName())) {
                p.sendMessage(mini.deserialize("<red>Stand on your claimed mapart to use this.</red>"));
                e.setCancelled(true);
                return;
            }
            if (!data.isOwner(region.getName(), p.getUniqueId()) && !data.isTrusted(region.getName(), p.getUniqueId())) {
                p.sendMessage(mini.deserialize("<red>You must be the owner or trusted on this mapart.</red>"));
                e.setCancelled(true);
                return;
            }

            long now = System.currentTimeMillis();
            Long last = rcCd.get(p.getUniqueId());
            if (last != null && now - last < CLEAR_COOLDOWN_MS) {
                e.setCancelled(true);
                return;
            }
            rcCd.put(p.getUniqueId(), now);

            Material target = selection.get(p.getUniqueId());
            if (target == null) {
                p.sendMessage(mini.deserialize("<yellow>Left-click a block to select it first.</yellow>"));
                e.setCancelled(true);
                return;
            }
            if (isProtectedMaterial(target)) {
                p.sendMessage(mini.deserialize("<red>You cannot clear that block type with this wand.</red>"));
                e.setCancelled(true);
                return;
            }

            int capacity = computeCapacity(p.getInventory(), target);
            if (capacity <= 0) {
                p.sendMessage(mini.deserialize("<red>Your inventory is full.</red>"));
                e.setCancelled(true);
                return;
            }
            int limit = Math.min(64, capacity);

            List<Block> toClear = findBlocks(region, p.getWorld(), target, limit);
            if (toClear.isEmpty()) {
                p.sendMessage(mini.deserialize("<yellow>No ")
                        .append(mini.deserialize("<#c1adfe>" + target.name() + "</#c1adfe>"))
                        .append(mini.deserialize(" found on this mapart.</yellow>")));
                e.setCancelled(true);
                return;
            }

            for (Block b : toClear) b.setType(Material.AIR, false);
            giveBlocks(p.getInventory(), target, toClear.size());
            p.sendMessage(mini.deserialize("<green>Cleared </green><#c1adfe>" + toClear.size() + "</#c1adfe><green> " + target.name() + ".</green>"));
            e.setCancelled(true);
        }
    }

    // --- Robust wand detection ---
    private boolean matchesWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        // 1) Use your existing utility (handles §-colors & italics-off normalization)
        if (LoreUtil.itemHasLore(item, LORE_ID)) return true;

        // 2) Try Component lore > plain text fallback ("Mapart Clearing Wand")
        var meta = item.getItemMeta();
        List<Component> comp = meta.lore();
        if (comp != null) {
            for (Component c : comp) {
                String pl = plain.serialize(c).trim();
                if (pl.equalsIgnoreCase(DISPLAY_TEXT)) return true;

                // Also tolerate any §-serialized variant that ends with the words
                String lg = legacy.serialize(c);
                if (lg.endsWith(DISPLAY_TEXT)) return true;
            }
        }

        // 3) Try legacy String lore (older items)
        @SuppressWarnings("deprecation")
        List<String> legacyLore = meta.hasLore() ? meta.getLore() : null;
        if (legacyLore != null) {
            for (String s : legacyLore) {
                if (s == null) continue;
                // Normalize &-codes/§-codes to Component -> plain for a robust compare
                String normalizedPlain = plain.serialize(ampersand.deserialize(s)).trim();
                if (normalizedPlain.equalsIgnoreCase(DISPLAY_TEXT)) return true;

                // Or direct §-string endsWith
                String normSect = legacy.serialize(ampersand.deserialize(s));
                if (normSect.endsWith(DISPLAY_TEXT)) return true;
            }
        }

        return false;
    }

    private int computeCapacity(Inventory inv, Material mat) {
        int cap = 0;
        for (ItemStack s : inv.getStorageContents()) {
            if (s == null || s.getType().isAir()) {
                cap += mat.getMaxStackSize();
            } else if (s.getType() == mat) {
                cap += Math.max(0, mat.getMaxStackSize() - s.getAmount());
            }
            if (cap >= 64) return 64;
        }
        return cap;
    }

    private void giveBlocks(Inventory inv, Material mat, int amount) {
        int left = amount;
        while (left > 0) {
            int give = Math.min(mat.getMaxStackSize(), left);
            Map<Integer, ItemStack> overflow = inv.addItem(new ItemStack(mat, give));
            if (!overflow.isEmpty()) break;
            left -= give;
        }
    }

    private List<Block> findBlocks(MapArtRegion r, World w, Material target, int limit) {
        List<Block> list = new ArrayList<>(limit);
        for (int x = r.getMinX(); x <= r.getMaxX() && list.size() < limit; x++) {
            for (int y = r.getMinY(); y <= r.getMaxY() && list.size() < limit; y++) {
                for (int z = r.getMinZ(); z <= r.getMaxZ(); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() != target) continue;
                    if (isProtectedBlock(b)) continue;
                    list.add(b);
                    if (list.size() >= limit) break;
                }
            }
        }
        return list;
    }

    private boolean isProtectedBlock(Block b) {
        Material m = b.getType();
        if (m == Material.BARRIER) return true;
        if (b.getState() instanceof Container) return true;
        return isRedstoneExceptBlock(m);
    }

    private boolean isProtectedMaterial(Material m) {
        if (m == Material.BARRIER) return true;
        return isRedstoneExceptBlock(m);
    }

    private boolean isRedstoneExceptBlock(Material m) {
        if (m == Material.REDSTONE_BLOCK) return false;
        String n = m.name();
        if (n.contains("REDSTONE")) return true;
        if (n.contains("REPEATER")) return true;
        if (n.contains("COMPARATOR")) return true;
        if (n.contains("OBSERVER")) return true;
        if (n.contains("PISTON")) return true;
        if (n.contains("DISPENSER")) return true;
        if (n.contains("DROPPER")) return true;
        if (n.contains("HOPPER")) return true;
        if (n.contains("LEVER")) return true;
        if (n.contains("BUTTON")) return true;
        if (n.contains("PRESSURE_PLATE")) return true;
        if (n.contains("TRIPWIRE")) return true;
        if (n.contains("DAYLIGHT_DETECTOR")) return true;
        if (n.contains("SCULK_SENSOR")) return true;
        if (n.contains("TARGET")) return true;
        if (n.contains("NOTE_BLOCK")) return true;
        return n.contains("LAMP");
    }
}
