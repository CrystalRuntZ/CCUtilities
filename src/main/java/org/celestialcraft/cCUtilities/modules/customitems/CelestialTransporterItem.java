package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class CelestialTransporterItem implements CustomItem {

    private static final String LORE_LINE = "§7Celestial Transporter";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final NamespacedKey ID_KEY = new NamespacedKey(CCUtilities.getInstance(), "celestial_transporter_id");
    private static final NamespacedKey TYPE_KEY = new NamespacedKey(CCUtilities.getInstance(), "celestial_transporter_type");
    private static final NamespacedKey NAME_KEY = new NamespacedKey(CCUtilities.getInstance(), "celestial_transporter_name");

    private static final Set<String> BLOCKED_TYPES = Set.of(
            "villager", "iron_golem", "wither", "ender_dragon", "warden", "clayfish"
    );

    private static final Set<String> BLOCKED_WORLDS = Set.of(
            "spawnworld", "shops", "flat", "mapart"
    );

    @Override
    public String getIdentifier() {
        return "celestial_transporter";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    // For left-click release (interact with air/block)
    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!event.getAction().isLeftClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        assert item != null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(TYPE_KEY, PersistentDataType.STRING) || !container.has(ID_KEY, PersistentDataType.STRING)) {
            player.sendMessage(Component.text("There is no stored entity to release!").color(TextColor.color(0xC1ADFE)));
            return;
        }

        String worldName = player.getWorld().getName().toLowerCase();
        if (BLOCKED_WORLDS.contains(worldName)) {
            player.sendMessage(Component.text("You cannot release entities in this world!").color(TextColor.color(0xC1ADFE)));
            return;
        }

        String type = container.get(TYPE_KEY, PersistentDataType.STRING);
        String name = container.get(NAME_KEY, PersistentDataType.STRING);
        String id = container.get(ID_KEY, PersistentDataType.STRING);

        EntityType entityType;
        try {
            assert type != null;
            entityType = EntityType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Stored entity type is invalid. Release cancelled.").color(TextColor.color(0xC1ADFE)));
            container.remove(TYPE_KEY);
            container.remove(ID_KEY);
            container.remove(NAME_KEY);
            return;
        }

        Entity spawned = player.getWorld().spawnEntity(player.getLocation(), entityType);
        if (spawned instanceof LivingEntity living) {
            if (name != null) living.customName(LegacyComponentSerializer.legacySection().deserialize(name));
            double maxHealth = Objects.requireNonNull(living.getAttribute(Attribute.MAX_HEALTH)).getValue();
            living.setHealth(maxHealth);
        }
        spawned.setMetadata("celestial_remove", new FixedMetadataValue(CCUtilities.getInstance(), "restored"));

        container.remove(TYPE_KEY);
        container.remove(ID_KEY);
        container.remove(NAME_KEY);

        meta.lore(List.of(
                LORE_COMPONENT,
                LegacyComponentSerializer.legacySection().deserialize("§8Stored:"),
                LegacyComponentSerializer.legacySection().deserialize("§8ID: " + id)
        ));
        item.setItemMeta(meta);

        player.sendMessage(Component.text("Entity released!").color(TextColor.color(0xC1ADFE)));
    }

    // For right-click on an entity (store it)
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        if (!(event.getRightClicked() instanceof LivingEntity entity)) return;
        if (entity instanceof Player || entity.getHealth() <= 0) return;

        String worldName = player.getWorld().getName().toLowerCase();
        if (BLOCKED_WORLDS.contains(worldName)) {
            player.sendMessage(Component.text("You cannot store entities in this world!").color(TextColor.color(0xC1ADFE)));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(TYPE_KEY, PersistentDataType.STRING)) {
            player.sendMessage(Component.text("This item already has a stored entity!").color(TextColor.color(0xC1ADFE)));
            return;
        }

        String rawType = entity.getType().name().toLowerCase(Locale.ROOT);
        if (rawType.equals("snow_fox") || rawType.equals("red_fox")) rawType = "fox";
        if (BLOCKED_TYPES.contains(rawType)) {
            player.sendMessage(Component.text("You cannot store a " + rawType + " with this item!").color(TextColor.color(0xC1ADFE)));
            return;
        }

        String uuid = UUID.randomUUID().toString();
        String displayName = entity.getName();

        container.set(ID_KEY, PersistentDataType.STRING, uuid);
        container.set(TYPE_KEY, PersistentDataType.STRING, rawType);
        container.set(NAME_KEY, PersistentDataType.STRING, displayName);

        meta.lore(List.of(
                LORE_COMPONENT,
                LegacyComponentSerializer.legacySection().deserialize("§8Stored: " + rawType),
                LegacyComponentSerializer.legacySection().deserialize("§8ID: " + uuid)
        ));
        item.setItemMeta(meta);

        entity.setMetadata("celestial_remove", new FixedMetadataValue(CCUtilities.getInstance(), uuid));
        entity.remove();

        player.sendMessage(Component.text("Stored a " + rawType + "!").color(TextColor.color(0xC1ADFE)));
    }

    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("celestial_remove")) {
            event.getDrops().clear();
            LivingEntity living = event.getEntity();
            Objects.requireNonNull(living.getEquipment()).clear();
        }
    }
}
