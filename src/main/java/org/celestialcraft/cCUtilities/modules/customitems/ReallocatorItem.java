package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ReallocatorItem implements CustomItem {

    private static final String RAW_LORE = "&7Reallocator";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private static final long COOLDOWN_MILLIS = 30_000;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "reallocator_item";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.getType().isItem() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    private boolean hasEquipped(Player player) {
        return matches(player.getInventory().getItemInMainHand()) ||
                matches(player.getInventory().getItemInOffHand());
    }

    @Override
    public void onFallDamage(Player player, EntityDamageEvent event) {
        if (!hasEquipped(player)) return;

        double healthBefore = player.getHealth();
        double healthAfter = healthBefore - event.getFinalDamage();

        if (healthBefore > 8.0 && healthAfter <= 8.0) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < COOLDOWN_MILLIS) {
                return;
            }

            cooldowns.put(uuid, now);

            double maxHealth = Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue();
            double newHealth = Math.min(maxHealth, healthAfter + 8.0);
            player.setHealth(newHealth);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 0));
        }
    }
}
