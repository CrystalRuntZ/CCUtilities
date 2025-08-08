package org.celestialcraft.cCUtilities.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;

import java.util.Objects;
import java.util.Set;

public class ShopChestListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    private final Set<String> validCurrencies = Set.of(
            "DIAMOND", "DIAMONDBLOCK", "EMERALD", "EMERALDBLOCK",
            "IRONINGOT", "IRONBLOCK", "GOLDINGOT", "GOLDBLOCK",
            "NETHERITEINGOT", "NETHERITEBLOCK", "AMETHYST",
            "COPPERINGOT", "COPPERBLOCK", "COAL", "COALBLOCK",
            "REDSTONE", "REDSTONEBLOCK", "LAPIS", "LAPISBLOCK"
    );

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!ModuleManager.isEnabled("playershops")) return;

        var player = event.getPlayer();
        var block = event.getBlock();
        Block attached = getAttachedBlock(block);
        if (attached == null) return;

        boolean isValidBlock = switch (attached.getType()) {
            case CHEST, TRAPPED_CHEST, BARREL,
                 SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX,
                 CYAN_SHULKER_BOX, GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX,
                 LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX,
                 PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> true;
            default -> false;
        };

        if (!isValidBlock) return;

        if (attached.getState() instanceof DoubleChest) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-no-double-chests")));
            return;
        }

        String line1 = event.line(0) != null ? plain.serialize(Objects.requireNonNull(event.line(0))).trim() : null;
        String line2 = event.line(1) != null ? plain.serialize(Objects.requireNonNull(event.line(1))).trim() : null;
        String line3 = event.line(2) != null ? plain.serialize(Objects.requireNonNull(event.line(2))).trim().toUpperCase() : null;

        if (line1 == null || line2 == null || line3 == null) return;
        if (!line1.equals("[PRICE]")) return;

        int amount;
        try {
            amount = Integer.parseInt(line2);
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-amount")));
            return;
        }

        if (amount < 1 || amount > 64) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-amount")));
            return;
        }

        if (!validCurrencies.contains(line3)) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-invalid-currency")));
            return;
        }

        var region = ShopDataManager.getRegionAt(block.getLocation());
        if (region == null) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-not-in-region")));
            return;
        }

        var owner = ShopDataManager.getClaim(player.getUniqueId());
        boolean isTrusted = ShopDataManager.getTrusted(region.name()).contains(player.getUniqueId());

        if (!region.name().equals(owner) && !isTrusted) {
            player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-not-owner")));
            return;
        }

        event.line(3, Component.text(player.getName()));
        player.sendMessage(mm.deserialize(MessageConfig.get("playershops.message-chest-created")));
    }

    private Block getAttachedBlock(Block sign) {
        var data = sign.getBlockData();
        if (data instanceof Directional directional) {
            return sign.getRelative(directional.getFacing().getOppositeFace());
        }
        return null;
    }
}
