package org.celestialcraft.cCUtilities.modules.referral;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.*;

public class ReferralLeaderboardGUI implements Listener {
    private final ReferralDatabase db;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ReferralLeaderboardGUI(ReferralDatabase db) {
        this.db = db;
    }

    public void open(Player player) {
        String rawTitle = MessageConfig.get("referral.gui-title");
        Component title = mini.deserialize(rawTitle);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        Map<String, Integer> top = db.getTopReferrers(9);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(top.entrySet());

        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text(" "));
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        Map<Integer, Integer> positions = Map.of(
                0, 4, 1, 12, 2, 13, 3, 14, 4, 11,
                5, 12, 6, 13, 7, 14, 8, 15
        );

        for (int index = 0; index < sorted.size(); index++) {
            Map.Entry<String, Integer> entry = sorted.get(index);
            if (!positions.containsKey(index)) continue;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());

            if (meta != null) {
                meta.setOwningPlayer(offline);
                meta.displayName(Component.text("#" + (index + 1) + " " + entry.getKey(), NamedTextColor.GOLD));
                meta.lore(List.of(Component.text("Referrals: " + entry.getValue(), NamedTextColor.AQUA)));
                skull.setItemMeta(meta);
            }

            inv.setItem(positions.get(index), skull);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String expectedRaw = MessageConfig.get("referral.gui-title");
        Component expectedTitle = mini.deserialize(expectedRaw);
        String expectedPlain = PlainTextComponentSerializer.plainText().serialize(expectedTitle);
        String currentPlain = PlainTextComponentSerializer.plainText().serialize(e.getView().title());

        if (!currentPlain.equals(expectedPlain)) return;

        e.setCancelled(true);
    }
}
