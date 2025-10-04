package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MercyUltimateItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final NamespacedKey mercyKey;
    private final Set<UUID> notified = new HashSet<>();
    private final Map<UUID, Double> distanceMap = new HashMap<>();

    public MercyUltimateItem(JavaPlugin plugin) {
        this.mercyKey = new NamespacedKey(plugin, "mercy_charge");
    }

    @Override
    public String getIdentifier() {
        return "mercy_ultimate";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        List<Component> loreLines = item.getItemMeta().lore();
        if (loreLines == null) return false;
        for (Component line : loreLines) {
            if ("§7Mercy Ultimate".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isValid() || player.isDead()) return;
        if (player.isInsideVehicle()) return;
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;
        if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return;

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0) return;

        ItemStack item = getFirstChargingMercyItem(player);
        if (item == null || !item.hasItemMeta()) {
            player.sendActionBar(Component.empty());
            notified.remove(player.getUniqueId());
            distanceMap.remove(player.getUniqueId());
            return;
        }

        UUID uuid = player.getUniqueId();
        double accumulated = distanceMap.getOrDefault(uuid, 0.0) + distance;

        if (accumulated < 100.0) {
            distanceMap.put(uuid, accumulated);

            // Show charge progress in action bar
            int oldCharge = getCharge(item);
            int gain = (int) (accumulated / 100.0);
            int newCharge = Math.min(100, oldCharge + gain);
            Component progressMsg = Component.text("Mercy Ultimate Charge: ")
                    .color(TextColor.color(0xAAAAAA))
                    .append(Component.text(newCharge + "%").color(TextColor.fromHexString("#c1adfe")));
            player.sendActionBar(progressMsg);

            return;
        }

        int gain = (int) (accumulated / 100.0);
        accumulated %= 100.0;
        distanceMap.put(uuid, accumulated);

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        int old = container.getOrDefault(mercyKey, PersistentDataType.INTEGER, 0);
        int updated = Math.min(100, old + gain);

        if (updated != old) {
            List<Component> existingLore = meta.lore();
            List<Component> lore = (existingLore != null) ? new ArrayList<>(existingLore) : new ArrayList<>();

            if (lore.isEmpty()) {
                lore.add(Component.text("Mercy Ultimate").color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
            }
            if (lore.size() < 2) {
                lore.add(Component.empty());
            }

            lore.set(1, Component.text("Charge: ").color(TextColor.color(0xAAAAAA))
                    .append(Component.text(updated + "%").color(TextColor.fromHexString("#c1adfe")))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            container.set(mercyKey, PersistentDataType.INTEGER, updated);
            item.setItemMeta(meta);
        }

        if (updated == 100) {
            if (notified.add(uuid)) {
                player.sendActionBar(Component.text("💜 Mercy Ultimate fully charged!").color(TextColor.fromHexString("#c1adfe")));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            } else {
                // Continuously show charged state on action bar
                player.sendActionBar(Component.text("💜 Mercy Ultimate fully charged!").color(TextColor.fromHexString("#c1adfe")));
            }
        } else {
            notified.remove(uuid);
        }
    }

    @Override
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = getFirstChargedMercyItem(player);
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Mercy Ultimate").color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Charge: ").color(TextColor.color(0xAAAAAA))
                .append(Component.text("0%").color(TextColor.fromHexString("#c1adfe")))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(mercyKey, PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);

        notified.remove(player.getUniqueId());

        if (player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return;
        }

        event.setCancelled(false);
    }

    private int getCharge(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        var container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(mercyKey, PersistentDataType.INTEGER, 0);
    }

    private ItemStack getFirstChargingMercyItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            int charge = container.getOrDefault(mercyKey, PersistentDataType.INTEGER, 0);
            if (matches(item) && charge < 100) {
                return item;
            }
        }
        return null;
    }

    private ItemStack getFirstChargedMercyItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            int charge = container.getOrDefault(mercyKey, PersistentDataType.INTEGER, 0);
            if (matches(item) && charge >= 100) {
                return item;
            }
        }
        return null;
    }
}
