package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlippingWandItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final NamespacedKey FLIPPED_KEY = new NamespacedKey("celestialutilities", "flipping_wand_tag");

    @Override
    public String getIdentifier() {
        return "flipping_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;

        var meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null) return false;

        for (var line : lore) {
            if ("§7Flipping Wand".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!matches(held)) return;
        if (isOnCooldown(player.getUniqueId())) {
            player.sendActionBar(Component.text("Flipping Wand is cooling down. Please wait.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        toggleUpsideDown(player);
        setCooldown(player.getUniqueId());
        player.sendActionBar(Component.text("You flipped yourself!").color(TextColor.color(0x55FF55)));
    }

    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player user = event.getPlayer();
        ItemStack held = user.getInventory().getItemInMainHand();
        if (!matches(held)) return;

        Entity target = event.getRightClicked();
        if (target instanceof Player) return;
        if (isOnCooldown(user.getUniqueId())) {
            user.sendActionBar(Component.text("Flipping Wand is cooling down. Please wait.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        toggleUpsideDown(target);
        setCooldown(user.getUniqueId());
        user.sendActionBar(Component.text("You flipped the entity!").color(TextColor.color(0x55FF55)));
    }

    private void toggleUpsideDown(Entity entity) {
        if (isFlipped(entity)) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().remove(FLIPPED_KEY);
        } else {
            entity.customName(Component.text("Dinnerbone"));
            entity.setCustomNameVisible(true);
            Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> entity.setCustomNameVisible(false), 2L);
            entity.getPersistentDataContainer().set(FLIPPED_KEY, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private boolean isFlipped(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(FLIPPED_KEY, PersistentDataType.BYTE);
    }

    private void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    private boolean isOnCooldown(UUID uuid) {
        Long last = cooldowns.get(uuid);
        return last != null && (System.currentTimeMillis() - last) < 10_000;
    }
}
