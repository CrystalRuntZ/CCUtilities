package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.*;

public class ForestBiomeWand implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Forest Biome Wand";
    private static final long COOLDOWN_MS = 5000;
    private static final String ALLOWED_WORLD = "wild";

    private static final List<Biome> BIOMES = List.of(
            Biome.FOREST,
            Biome.DARK_FOREST,
            Biome.BIRCH_FOREST,
            Biome.FLOWER_FOREST,
            Biome.OLD_GROWTH_BIRCH_FOREST,
            Biome.CHERRY_GROVE,
            Biome.TAIGA,
            Biome.OLD_GROWTH_SPRUCE_TAIGA,
            Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.SNOWY_TAIGA
    );

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Integer> biomeSelections = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "forest_biome_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        String formatted = LORE_IDENTIFIER.replace("&", "§");
        for (Component line : lore) {
            if (legacy.serialize(line).equalsIgnoreCase(formatted)) {
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

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            handleLeftClick(player);
        } else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            handleRightClick(player);
        }
    }

    private void handleLeftClick(Player player) {
        UUID uuid = player.getUniqueId();
        int index = (biomeSelections.getOrDefault(uuid, 0) + 1) % BIOMES.size();
        biomeSelections.put(uuid, index);
        Biome biome = BIOMES.get(index);

        NamespacedKey key = (NamespacedKey) biome.key();

        String formattedName = key.getKey().replace('_', ' ');

        Component title = Component.text("Biome: ", NamedTextColor.GRAY)
                .append(Component.text(formattedName, TextColor.color(0xC1ADFE)));

        player.showTitle(Title.title(title, Component.empty(),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(800), Duration.ofMillis(200))));
    }

    private void handleRightClick(Player player) {
        World world = player.getWorld();
        if (!world.getName().equalsIgnoreCase(ALLOWED_WORLD)) {
            player.sendMessage(Component.text("You can only use this item in the wilderness!", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        Biome selectedBiome = BIOMES.get(biomeSelections.getOrDefault(uuid, 0));
        Location loc = player.getLocation();
        int chunkX = loc.getChunk().getX() << 4;
        int chunkZ = loc.getChunk().getZ() << 4;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    world.setBiome(chunkX + x, y, chunkZ + z, selectedBiome);
                }
            }
        }
    }
}
