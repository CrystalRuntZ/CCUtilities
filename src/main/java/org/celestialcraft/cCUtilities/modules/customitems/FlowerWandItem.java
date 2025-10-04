package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FlowerWandItem implements CustomItem {

    private static final String LORE_LINE = "§7Flower Wand";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    // Cooldown per player UUID
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 2000; // 2 seconds

    // Possible flower types
    private static final Material[] FLOWERS = {
            Material.POPPY, Material.DANDELION, Material.AZURE_BLUET, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
            Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
    };

    @Override
    public String getIdentifier() {
        return "flower_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) {
            player.sendActionBar(Component.text("Wait before using Flower Wand again")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        cooldowns.put(player.getUniqueId(), now);

        // Loop over blocks in radius 5
        Block center = player.getLocation().getBlock();
        int radius = 5;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) { // small vertical range
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.getRelative(x, y, z);
                    if (block.getType() == Material.AIR) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType() == Material.GRASS_BLOCK) {
                            int chance = random.nextInt(1, 101); // 1–100
                            if (chance <= 20) { // 20% chance
                                Material flower = FLOWERS[random.nextInt(FLOWERS.length)];
                                block.setType(flower);
                            }
                        }
                    }
                }
            }
        }
    }
}
