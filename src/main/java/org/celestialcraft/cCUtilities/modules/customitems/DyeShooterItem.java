package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DyeShooterItem implements CustomItem {

    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize("§7Dye Shooter");
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final List<Material> dyeTypes = List.of(
            Material.BLACK_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.CYAN_DYE,
            Material.GRAY_DYE, Material.GREEN_DYE, Material.LIGHT_BLUE_DYE, Material.LIGHT_GRAY_DYE,
            Material.LIME_DYE, Material.MAGENTA_DYE, Material.ORANGE_DYE, Material.PINK_DYE,
            Material.PURPLE_DYE, Material.RED_DYE, Material.WHITE_DYE, Material.YELLOW_DYE
    );
    private final Random random = new Random();

    @Override
    public String getIdentifier() {
        return "dye_shooter";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> Component.text().append(line).build().equals(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < 3000) return;
        cooldowns.put(uuid, now);

        Location eyeLoc = player.getEyeLocation();
        Material dyeType = dyeTypes.get(random.nextInt(dyeTypes.size()));
        ItemStack dyeItem = new ItemStack(dyeType);

        Item thrown = player.getWorld().dropItem(eyeLoc, dyeItem);
        thrown.setVelocity(eyeLoc.getDirection().normalize().multiply(1.5));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);
        player.getWorld().spawnParticle(Particle.DUST, eyeLoc, 15, 0.1, 0.1, 0.1,
                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1));
    }
}
