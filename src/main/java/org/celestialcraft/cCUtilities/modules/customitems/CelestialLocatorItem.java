package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CelestialLocatorItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Celestial Locator";
    private static final long COOLDOWN_MS = 10_000;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "celestial_locator";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        if (!player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid) < COOLDOWN_MS)) {
            long remaining = (COOLDOWN_MS - (now - cooldowns.get(uuid))) / 1000;
            player.sendActionBar(Component.text("Cooldown active: " + remaining + "s").color(TextColor.color(0xFF5555)));
            return;
        }
        cooldowns.put(uuid, now);

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!other.getWorld().equals(player.getWorld())) continue;

            double distance = player.getLocation().distance(other.getLocation());
            if (distance <= 5000 && distance < nearestDistance) {
                nearest = other;
                nearestDistance = distance;
            }
        }

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500), // fade in
                Duration.ofMillis(3000), // stay
                Duration.ofMillis(500)   // fade out
        );

        Component title;
        Component subtitle;
        if (nearest != null) {
            title = Component.text(nearest.getName()).color(TextColor.color(0xAAAAAA));
            subtitle = Component.text(((int) nearestDistance) + " blocks").color(TextColor.color(0xC11AFE));
        } else {
            title = Component.text("No players nearby").color(TextColor.color(0xAAAAAA));
            subtitle = Component.empty();
        }
        player.showTitle(Title.title(title, subtitle, times));
    }
}
