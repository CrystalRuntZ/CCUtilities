package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

public class ShopChestAccessListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlot() < 0 || event.getCurrentItem() == null) return;

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!ShopUtils.isShopChest(holder)) return;

        Sign shopSign = ShopUtils.getAttachedSign(holder);
        if (shopSign == null) return;

        var signLines = shopSign.getSide(Side.FRONT);

        String priceLine = plain.serialize(signLines.line(1));
        String currencyLine = plain.serialize(signLines.line(2));
        String ownerLine = plain.serialize(signLines.line(3));

        Integer price = tryParseInt(priceLine);
        if (price == null) return;

        Material currencyType = Material.matchMaterial(currencyLine.toUpperCase());
        if (currencyType == null) return;

        boolean isOwner = player.getName().equalsIgnoreCase(ownerLine);
        boolean hasBypass = player.hasPermission("shops.chest.bypass");

        if (!isOwner && !hasBypass) {
            int currencyAmount = ShopUtils.countItems(player.getInventory(), currencyType);
            if (currencyAmount < price) {
                String msg = MessageConfig.get("playershops.message-insufficient-currency")
                        .replace("%price%", price.toString())
                        .replace("%currency%", currencyType.name().toLowerCase());

                player.sendMessage(mm.deserialize(msg));
                event.setCancelled(true);
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            ItemStack clone = clickedItem.clone();
            clone.setAmount(1);

            ShopUtils.removeItems(player.getInventory(), currencyType, price);
            player.getInventory().addItem(clone);

            String msg = MessageConfig.get("playershops.message-purchase-success")
                    .replace("%item%", clone.getType().name().toLowerCase())
                    .replace("%price%", price.toString())
                    .replace("%currency%", currencyType.name().toLowerCase());

            player.sendMessage(mm.deserialize(msg));
            event.setCancelled(true);
        }
    }

    private Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
