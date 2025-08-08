package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.*;

public class PotOfGreedItem implements CustomItem {

    private static final String IDENTIFIER = "§7Pot of Greed";
    private static final String UNUSED_LINE = "Unused";
    private static final String USED_LINE = "§8Used";

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "pot_of_greed";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> serializer.serialize(line).equals(IDENTIFIER));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        if (!ClaimUtils.canBuild(player, player.getLocation())) return;

        assert item != null;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.size() < 2) return;

        String line = serializer.serialize(lore.get(1)).replace("§", "").trim();
        if (!line.equalsIgnoreCase(UNUSED_LINE)) {
            player.sendMessage(Component.text("§cThis Pot of Greed has already been used!"));
            return;
        }

        // Mark as used
        lore.set(1, serializer.deserialize(USED_LINE));
        meta.lore(lore);
        item.setItemMeta(meta);

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        Collections.shuffle(online);

        List<Player> selected = new ArrayList<>();
        selected.add(player);
        for (Player p : online) {
            if (selected.size() >= 3) break;
            selected.add(p);
        }

        int rewardNumber = 1 + (int) (Math.random() * 10);
        int amount;
        switch (rewardNumber) {
            case 4 -> amount = 64;
            case 5 -> amount = 32;
            case 8 -> amount = 16;
            default -> amount = 1;
        }

        String commandTemplate = "si give pog" + rewardNumber + " " + amount + " ";

        for (Player p : selected) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandTemplate + p.getName());
        }

        String hex = "§x§C§1§A§F§D§E";
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            names.append(hex).append(selected.get(i).getName());
            if (i < selected.size() - 1) names.append("§7, ");
        }

        Bukkit.broadcast(Component.text(hex + "☆ §7The following players, " +
                names + "§7, have won a " + hex + "Pot of Greed §7reward!"));
    }
}
