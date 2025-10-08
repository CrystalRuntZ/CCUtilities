package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HalloweenRankTokenItem implements CustomItem {

    private static final String LORE_LINE = "§7Halloween Rank Token";

    private static final List<String> PREFIXES = List.of(
            "&#861722&lV&#762C33&lA&#654044&lM&#555555&lP&#654044&lI&#762C33&lR&#861722&lE &#861722",
            "&#436936&lV&#3D6650&lA&#38636B&lM&#326085&lP&#4C7578&lI&#678A6A&lR&#819F5D&lE &#819F5D",
            "&#02AC00&lF&#15B400&lR&#28BB00&lA&#3BC300&lN&#4ECA00&lK&#61D200&lE&#73D900&lN&#86E100&lS&#99E800&lT&#ACF000&lE&#BFF700&lI&#D2FF00&lN &#D2FF00",
            "&#CBCBCB&lH&#B48F93&lA&#9D535A&lU&#861722&lN&#7A2230&lT&#6E2E3F&lE&#62394D&lD &#62394D",
            "&#E5D7D9&lS&#C1B7B8&lK&#9D9697&lE&#797676&lL&#555555&lE&#858081&lT&#B5ACAD&lO&#E5D7D9&lN &#E5D7D9",
            "&#A5F1AD&lG&#C6EAC3&lH&#E7E3D9&lO&#C6EAC3&lU&#A5F1AD&lL &#A5F1AD",
            "&#5180A5&lP&#7499B7&lO&#97B3C9&lL&#B9CCDB&lT&#DCE6ED&lE&#FFFFFF&lR&#E2DFE8&lG&#C6BFD0&lE&#A99EB9&lI&#8D7EA1&lS&#705E8A&lT &#705E8A",
            "&#645E8A&lP&#7D6FA2&lO&#9581BA&lS&#AE92D1&lS&#C6A3E9&lE&#AE92D1&lS&#9581BA&lS&#7D6FA2&lE&#645E8A&lD &#645E8A",
            "&#C1053B&lK&#B93C60&lI&#B27385&lL&#AAAAAA&lL&#B65873&lE&#C1053B&lR &#C1053B",
            "&#FF6700&lP&#FE8A1F&lU&#FDAD3D&lM&#FCD05C&lP&#FDAD3D&lK&#FE8A1F&lI&#FF6700&lN &#FF6700",
            "&#8C66C6&lR&#715E8E&lA&#555555&lV&#715E8E&lE&#8C66C6&lN &#8C66C6",
            "&#6353CA&lR&#5854AB&lE&#4C548C&lA&#41556D&lP&#52549C&lE&#6353CA&lR &#6353CA"
    );

    @Override
    public String getIdentifier() {
        return "halloween_rank_token";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        String targetText = LegacyComponentSerializer.legacySection().serialize(Component.text(LORE_LINE)).trim().toLowerCase();

        for (Component line : lore) {
            if (line == null) continue;
            String lineText = LegacyComponentSerializer.legacySection().serialize(line).trim().toLowerCase();
            if (lineText.equals(targetText)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        String playerName = player.getName();

        int randIndex = ThreadLocalRandom.current().nextInt(PREFIXES.size());
        String chosenPrefix = PREFIXES.get(randIndex);

        String command = String.format("lp user %s meta settempprefix 6500 %s 7d", playerName, chosenPrefix);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage("§aHalloween rank prefix applied for 7 days!");

        event.setCancelled(true);
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        // No behavior on right-click without sneak
    }

}
