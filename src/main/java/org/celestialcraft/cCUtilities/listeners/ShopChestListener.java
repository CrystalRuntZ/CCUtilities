package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopRegion;
import org.celestialcraft.cCUtilities.utils.ShopUtils;

import java.util.regex.Pattern;

public class ShopChestListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;

        Player player = event.getPlayer();
        Block signBlock = event.getBlock();

        // --- Normalize line 1 to styled PRICE tag (visual sugar only) ---
        Component l0 = event.line(0);
        String l0Plain = compToString(l0);
        if (l0Plain != null) {
            String s = l0Plain.trim()
                    .replace("§", "")
                    .replace("<", "")
                    .replace(">", "");
            boolean isPriceWord = s.equalsIgnoreCase("price");
            boolean isBracketedPrice = Pattern.compile("\\[\\s*price\\s*]", Pattern.CASE_INSENSITIVE).matcher(s).matches();
            if (isPriceWord || isBracketedPrice) {
                event.line(0, mm.deserialize("<gray>[</gray><#c1adfe>PRICE</#c1adfe><gray>]</gray>"));
            }
        }

        // If editing an existing shop sign, enforce owner/bypass
        if (signBlock.getState() instanceof Sign existing) {
            Component first = existing.getSide(Side.FRONT).line(0);
            if ("[PRICE]".equalsIgnoreCase(plain.serialize(first).trim())) {
                String currentOwner = plain.serialize(existing.getSide(Side.FRONT).line(3)).trim();
                boolean ownerOrBypass = player.getName().equalsIgnoreCase(currentOwner) || player.hasPermission("shops.chest.bypass");
                if (!ownerOrBypass) {
                    event.setCancelled(true);
                    player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-not-owner")));
                    return;
                }
            }
        }

        // ---- Read lines and early-out if not a shop sign ----
        String line1 = compToString(event.line(0));
        String line2 = compToString(event.line(1));
        String line3 = compToString(event.line(2));
        if (line1 == null || line2 == null || line3 == null) return;

        if (!line1.equalsIgnoreCase("[PRICE]")) {
            // Not a shop sign → do not run container/double-chest logic at all
            return;
        }

        // ---- From here on: treat as shop sign ONLY if attached WALL sign to a container ----
        Block attached = getAttachedBlock(signBlock); // wall-sign only
        if (attached == null || !(attached.getState() instanceof Container)) {
            // Not attached to a container → not a valid shop setup; do nothing (don’t cancel)
            return;
        }

        // Disallow double chests (single chest size is 27)
        if (attached.getState() instanceof Chest chest) {
            if (chest.getInventory().getSize() > 27) {
                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-double-chests")));
                cancelIfPossible(event);
                return;
            }
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(line2.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-amount")));
            cancelIfPossible(event);
            return;
        }
        if (amount < 1 || amount > 64) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-amount")));
            cancelIfPossible(event);
            return;
        }

        // Parse currency
        Material currency = ShopUtils.parseCurrency(line3);
        if (currency == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-currency")));
            cancelIfPossible(event);
            return;
        }

        // Must be inside a defined region the player owns or is trusted in
        ShopRegion region = ShopDataManager.getRegionAt(attached.getLocation());
        if (region == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-not-in-region")));
            cancelIfPossible(event);
            return;
        }

        String ownerShop = ShopDataManager.getClaim(player.getUniqueId());
        boolean isTrusted = ShopDataManager.isTrusted(region.name(), player.getUniqueId());
        if (!region.name().equalsIgnoreCase(ownerShop) && !isTrusted) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-not-owner")));
            cancelIfPossible(event);
            return;
        }

        // Stamp owner on line 4 and bump activity
        event.line(3, Component.text(player.getName()));
        ShopDataManager.setLastUpdated(region.name(), System.currentTimeMillis());
        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-chest-created")));
    }

    private String compToString(Component c) {
        return c != null ? PlainTextComponentSerializer.plainText().serialize(c).trim() : null;
    }

    private void cancelIfPossible(SignChangeEvent e) {
        if (e instanceof Cancellable c) c.setCancelled(true);
    }

    /** Returns the block a sign is attached to. Only WALL signs are treated as attached; standing signs return null. */
    private static Block getAttachedBlock(Block signBlock) {
        var data = signBlock.getBlockData();
        if (data instanceof org.bukkit.block.data.type.WallSign wall) {
            return signBlock.getRelative(wall.getFacing().getOppositeFace());
        }
        // Standing signs are NOT considered attached to containers for shop logic
        return null;
    }
}
