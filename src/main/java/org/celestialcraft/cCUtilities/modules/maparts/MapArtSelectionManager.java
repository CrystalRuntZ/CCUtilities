package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapArtSelectionManager {
    private static final Map<UUID, org.bukkit.Location> pos1 = new HashMap<>();
    private static final Map<UUID, org.bukkit.Location> pos2 = new HashMap<>();
    private static final MiniMessage mini = MiniMessage.miniMessage();
    private static final String WAND_NAME = "<#c1adfe>MapArt Wand";
    private static final String WAND_LORE = "<gray>Select corners with left/right click";

    public static void setPos1(Player p, org.bukkit.Location l) { pos1.put(p.getUniqueId(), l); }
    public static void setPos2(Player p, org.bukkit.Location l) { pos2.put(p.getUniqueId(), l); }
    public static org.bukkit.Location getPos1(Player p) { return pos1.get(p.getUniqueId()); }
    public static org.bukkit.Location getPos2(Player p) { return pos2.get(p.getUniqueId()); }
    public static void clear(Player p) { pos1.remove(p.getUniqueId()); pos2.remove(p.getUniqueId()); }

    public static void giveWand(Player p) {
        ItemStack hoe = new ItemStack(Material.GOLDEN_HOE); // was STONE_HOE
        ItemMeta meta = hoe.getItemMeta();
        meta.displayName(mini.deserialize(WAND_NAME));
        meta.lore(List.of(mini.deserialize(WAND_LORE)));
        hoe.setItemMeta(meta);
        p.getInventory().addItem(hoe);
        p.sendMessage(mini.deserialize("<gray>Given </gray><#c1adfe>MapArt Wand</#c1adfe><gray>.</gray>"));
    }
}
