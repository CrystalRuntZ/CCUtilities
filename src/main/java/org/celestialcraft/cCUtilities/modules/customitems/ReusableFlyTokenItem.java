package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ReusableFlyTokenItem implements CustomItem {

    private static final String LORE_LINE = "§7Reusable Fly Token";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final long COOLDOWN_MS = 60L * 60L * 1000L; // 1 hour in milliseconds
    private static final int MAX_USES = 12;

    private static final NamespacedKey USES_KEY = new NamespacedKey(CCUtilities.getInstance(), "flytoken_uses");
    private static final NamespacedKey COOLDOWN_KEY = new NamespacedKey(CCUtilities.getInstance(), "flytoken_lastuse");

    @Override
    public String getIdentifier() {
        return "reusable_fly_token";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;

        String targetText = LegacyComponentSerializer.legacySection().serialize(LORE_COMPONENT).trim().toLowerCase();

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

        ItemStack inHand = event.getItem();
        if (!matches(inHand)) return;

        var player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        long now = System.currentTimeMillis();
        Long last = pdc.get(COOLDOWN_KEY, PersistentDataType.LONG);
        if (last != null) {
            long remaining = COOLDOWN_MS - (now - last);
            if (remaining > 0) {
                long secondsLeft = remaining / 1000;
                player.sendActionBar(Component.text("§cFly Token is on cooldown. " + secondsLeft + "s remaining."));
                event.setCancelled(true);
                return;
            }
        }

        Integer uses = pdc.get(USES_KEY, PersistentDataType.INTEGER);
        if (uses == null || uses >= MAX_USES) {
            uses = 0;
        }
        uses++;
        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        pdc.set(COOLDOWN_KEY, PersistentDataType.LONG, now);

        int usesLeft = MAX_USES - uses;
        player.sendMessage(MINI_MESSAGE.deserialize("<#C1AFDE>You have activated /fly for 1 hour! Uses left: <c1adfe>" + usesLeft));

        if (inHand != null && inHand.hasItemMeta()) {
            ItemMeta meta = inHand.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                else lore = new ArrayList<>(lore);

                if (lore.size() > 1) {
                    if (usesLeft > 0) {
                        lore.set(1, MINI_MESSAGE.deserialize("<gray>Uses Left:</gray> <#c1adfe>" + usesLeft + "</#c1adfe>"));
                    } else {
                        lore.remove(1);
                    }
                } else if (usesLeft > 0) {
                    lore.add(MINI_MESSAGE.deserialize("<gray>Uses Left:</gray> <#c1adfe>" + usesLeft + "</#c1adfe>"));
                }

                meta.lore(lore);
                inHand.setItemMeta(meta);
                player.updateInventory();
            }
        }

        try {
            String cmd = "lp user " + player.getName() + " permission settemp essentials.fly true 1h";
            CCUtilities.getInstance().getLogger().log(Level.INFO, "[FlyToken] Executing command: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception e) {
            CCUtilities.getInstance().getLogger().log(Level.SEVERE, "[FlyToken] Failed to execute LuckPerms command for player " + player.getName(), e);
        }

        if (uses >= MAX_USES) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>That was your final Fly Token use. The token has now been consumed."));
            assert inHand != null;
            if (inHand.getAmount() > 1) {
                inHand.setAmount(inHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();
        }

        event.setCancelled(true);
    }
}
