package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PixieDustItem implements CustomItem {

    private static final String LORE_LINE = "§7Pixie Dust";
    private static final long COOLDOWN_MS = 10 * 60 * 1000; // 10 minutes

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "pixie_dust";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        for (Component line : lore) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && cooldowns.get(uuid) > now) {
            long secondsLeft = (cooldowns.get(uuid) - now) / 1000;
            player.sendMessage("§cPixie Dust is recharging! " + secondsLeft + "s remaining.");
            event.setCancelled(true);
            return;
        }

        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
        player.setHealth(Math.min(player.getHealth() + 20.0, maxHealth));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30 * 20, 4)); // Speed V

        cooldowns.put(uuid, now + COOLDOWN_MS);
        player.sendMessage("§dPixie Dust activated! You've been healed and feel swift!");
        event.setCancelled(true);
    }
}