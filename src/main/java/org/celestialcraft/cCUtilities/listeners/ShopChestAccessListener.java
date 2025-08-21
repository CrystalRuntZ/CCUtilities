package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event.Result;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopChestAccessListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final JavaPlugin plugin;

    public ShopChestAccessListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 1) HIGHEST: explicitly allow right-click on valid shop chests for buyers (sign-present gate)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAllow(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = event.getClickedBlock();
        if (b == null) return;
        if (!(b.getState() instanceof Container container)) return;
        if (!ShopUtils.isShopChest(container)) return;

        // Determine ownership from the sign (line 4)
        Sign sign = ShopUtils.getAttachedSign(container);
        if (sign == null) return;
        Player p = event.getPlayer();
        String owner = plain.serialize(sign.getSide(Side.FRONT).line(3)).trim();
        boolean ownerOrBypass = p.getName().equalsIgnoreCase(owner)
                || p.hasPermission("shops.chest.bypass")
                || p.hasPermission("shops.admin");

        if (!ownerOrBypass) {
            // Allow using the chest; deny using the item in hand (prevents placing blocks on it)
            event.setCancelled(false);
            event.setUseInteractedBlock(Result.ALLOW);
            event.setUseItemInHand(Result.DENY);
        }
    }

    // 2) MONITOR: if something still denies the interaction, force-open the chest GUI
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractForceOpen(PlayerInteractEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = event.getClickedBlock();
        if (b == null) return;
        if (!(b.getState() instanceof Container container)) return;
        if (!ShopUtils.isShopChest(container)) return;

        Sign sign = ShopUtils.getAttachedSign(container);
        if (sign == null) return;

        Player p = event.getPlayer();
        String owner = plain.serialize(sign.getSide(Side.FRONT).line(3)).trim();
        boolean ownerOrBypass = p.getName().equalsIgnoreCase(owner)
                || p.hasPermission("shops.chest.bypass")
                || p.hasPermission("shops.admin");

        if (!ownerOrBypass) {
            @SuppressWarnings("deprecation")
            boolean denied = event.isCancelled()
                    || event.useInteractedBlock() == Result.DENY
                    || event.useItemInHand() == Result.DENY;

            if (denied) {
                // Open on next tick to bypass the denial
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(container.getInventory()));
            }
        }
    }

    // 3) If someone cancels the inventory open itself, un-cancel for shop chests (sign-present gate)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (holder == null) return;
        if (!ShopUtils.isShopChest(holder)) return;

        Sign sign = ShopUtils.getAttachedSign(holder);
        if (sign == null) return;

        String owner = plain.serialize(sign.getSide(Side.FRONT).line(3)).trim();
        boolean ownerOrBypass = p.getName().equalsIgnoreCase(owner)
                || p.hasPermission("shops.chest.bypass")
                || p.hasPermission("shops.admin");

        if (!ownerOrBypass && event.isCancelled()) {
            event.setCancelled(false);
        }
    }

    // Purchase flow (allow click-to-buy)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Container)) return; // shop creation forbids double chests
        if (!ShopUtils.isShopChest(holder)) return;

        // Owner/bypass can manage freely
        Sign sign = ShopUtils.getAttachedSign(holder);
        if (sign == null) return;
        String owner = plain.serialize(sign.getSide(Side.FRONT).line(3)).trim();

        boolean ownerOrBypass = player.getName().equalsIgnoreCase(owner) || player.hasPermission("shops.chest.bypass");

        // For non-owners: block management style interactions (shift-moves, swaps, collects, etc.)
        if (!ownerOrBypass) {
            InventoryAction action = event.getAction();
            switch (action) {
                case MOVE_TO_OTHER_INVENTORY,
                     HOTBAR_SWAP,
                     COLLECT_TO_CURSOR,
                     SWAP_WITH_CURSOR,
                     PLACE_ALL, PLACE_ONE, PLACE_SOME -> {
                    event.setCancelled(true);
                    return;
                }
                default -> {}
            }

            ClickType click = event.getClick();
            switch (click) {
                case NUMBER_KEY,
                     DOUBLE_CLICK,
                     MIDDLE,
                     CREATIVE,
                     SWAP_OFFHAND,
                     WINDOW_BORDER_LEFT,
                     WINDOW_BORDER_RIGHT -> {
                    event.setCancelled(true);
                    return;
                }
                default -> {}
            }
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
        }

        // Only handle purchases when clicking inside the chest area
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) return;

        // Need an item to buy
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // Parse sign: line1 price (int), line2 currency (material key/alias)
        String priceLine = safeLine(sign, 1);
        String currencyLine = safeLine(sign, 2);

        Integer price = tryParseInt(priceLine);
        if (price == null || price < 1) return;

        Material currencyType = ShopUtils.parseCurrency(currencyLine);
        if (currencyType == null) return;

        // If owner/bypass, let them manage normally
        if (ownerOrBypass) return;

        // Ensure the player isn’t holding something on the cursor (avoid weird swaps)
        ItemStack cursor = event.getCursor();
        if (!cursor.getType().isAir()) {
            event.setCancelled(true);
            return;
        }

        // Non-owner purchase flow: pay N currency into chest, receive 1 of clicked item
        if (ShopUtils.countItems(player.getInventory(), currencyType) < price) {
            // silent fail: just cancel the click without sending a message
            event.setCancelled(true);
            return;
        }

        ItemStack one = clicked.clone();
        one.setAmount(1);

        if (!ShopUtils.canFit(player.getInventory(), one)) {
            // keep these messages (not requested to remove)
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-inventory-full")));
            event.setCancelled(true);
            return;
        }

        ItemStack payment = new ItemStack(currencyType, price);
        if (!ShopUtils.canFit(top, payment)) {
            // keep these messages (not requested to remove)
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-chest-full")));
            event.setCancelled(true);
            return;
        }

        // Perform transaction (cancel default move and do it manually)
        event.setCancelled(true);

        // 1) Decrement the chest slot by 1
        clicked.setAmount(clicked.getAmount() - 1);
        if (clicked.getAmount() <= 0) {
            top.clear(raw);
        } else {
            top.setItem(raw, clicked);
        }

        // 2) Remove payment from player, add to chest
        ShopUtils.removeItems(player.getInventory(), currencyType, price);
        top.addItem(payment);

        // 3) Give item to player
        player.getInventory().addItem(one);

        // no success message (removed per request)
    }

    private String safeLine(Sign sign, int idx) {
        var comp = sign.getSide(Side.FRONT).line(idx);
        return plain.serialize(comp).trim();
    }

    private Integer tryParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }
}
