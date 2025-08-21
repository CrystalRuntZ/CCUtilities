package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Cancellable;
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

        // --- Normalize line 1 to styled PRICE tag ---
        Component l0 = event.line(0);
        String l0Plain = compToString(l0);
        if (l0Plain != null) {
            String s = l0Plain.trim()
                    .replace("§", "")
                    .replace("<", "")
                    .replace(">", "");
            boolean isPriceWord = s.equalsIgnoreCase("price");
            // Removed redundant escape for closing bracket
            boolean isBracketedPrice = Pattern.compile("\\[\\s*price\\s*]", Pattern.CASE_INSENSITIVE).matcher(s).matches();
            if (isPriceWord || isBracketedPrice) {
                event.line(0, mm.deserialize("<gray>[</gray><#c1adfe>PRICE</#c1adfe><gray>]</gray>"));
            }
        }

        // If this is editing an existing shop sign, enforce owner/bypass
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

        // Find the block the sign is attached to
        Block attached = getAttachedBlock(signBlock);
        if (!(attached.getState() instanceof Container)) return;

        // Disallow double chests (single chest size is 27)
        if (attached.getState() instanceof Chest chest) {
            if (chest.getInventory().getSize() > 27) {
                player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-double-chests")));
                cancelIfPossible(event);
                return;
            }
        }

        // Parse new sign lines (front side)
        String line1 = compToString(event.line(0));
        String line2 = compToString(event.line(1));
        String line3 = compToString(event.line(2));

        if (line1 == null || line2 == null || line3 == null) return;
        // Remove duplicate condition — plain text serializer yields "[PRICE]" for our styled tag
        if (!line1.equalsIgnoreCase("[PRICE]")) return;

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

        // Stamp owner name on line 4 and bump activity
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

    /** Returns the block a sign is attached to. Wall signs: opposite of facing; standing signs: below. */
    private static Block getAttachedBlock(Block signBlock) {
        var data = signBlock.getBlockData();
        try {
            if (data instanceof org.bukkit.block.data.type.WallSign wall) {
                return signBlock.getRelative(wall.getFacing().getOppositeFace());
            }
        } catch (NoClassDefFoundError ignored) { }
        return signBlock.getRelative(BlockFace.DOWN);
    }
}
