package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public class MapArtWandListener implements Listener {
    private final MiniMessage mini = MiniMessage.miniMessage();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;
        var item = e.getItem();
        if (item == null || item.getType() != Material.GOLDEN_HOE) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
        if (!plain.equalsIgnoreCase("MapArt Wand")) return;

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            MapArtSelectionManager.setPos1(e.getPlayer(), e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage(mini.deserialize("<#c1adfe>Pos1</#c1adfe><gray> set.</gray>"));
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            MapArtSelectionManager.setPos2(e.getPlayer(), e.getClickedBlock().getLocation());
            e.getPlayer().sendMessage(mini.deserialize("<#c1adfe>Pos2</#c1adfe><gray> set.</gray>"));
            e.setCancelled(true);
        }
    }
}
