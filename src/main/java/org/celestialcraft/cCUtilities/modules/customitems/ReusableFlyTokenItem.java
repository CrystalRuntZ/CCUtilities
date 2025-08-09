package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ReusableFlyTokenItem implements CustomItem {

    private static final String LORE_LINE = "§7Reusable Fly Token";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static final long COOLDOWN_MS = 60L * 60L * 1000L; // 1 hour

    private static final NamespacedKey USES_KEY =
            new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "flytoken_uses");
    private static final NamespacedKey COOLDOWN_KEY =
            new NamespacedKey(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), "flytoken_lastuse");

    @Override
    public String getIdentifier() {
        return "reusable_fly_token";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack inHand = event.getItem();
        if (!matches(inHand)) return;

        var player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        long now = System.currentTimeMillis();
        long last = pdc.getOrDefault(COOLDOWN_KEY, PersistentDataType.LONG, 0L);
        if (now - last < COOLDOWN_MS) {
            player.sendMessage(mm.deserialize("<red>Fly Token is on cooldown. Try again later."));
            return;
        }

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);
        uses++;
        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        pdc.set(COOLDOWN_KEY, PersistentDataType.LONG, now);

        player.sendMessage(mm.deserialize("<#C1AFDE>You have activated /fly for 1 hour! This token has been used " + uses + " times."));
        // LuckPerms temp permission for one hour
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "lp user " + player.getName() + " permission settemp essentials.fly true 1h");

        if (uses == 10) {
            player.sendMessage(mm.deserialize("<red>That was your final Fly Token use. The token has now been consumed."));
            // consume one token from main hand
            assert inHand != null;
            if (inHand.getAmount() > 1) {
                inHand.setAmount(inHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }
}
