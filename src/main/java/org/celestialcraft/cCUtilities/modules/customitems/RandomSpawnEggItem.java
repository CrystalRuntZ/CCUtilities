package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSpawnEggItem implements CustomItem {

    private static final String LORE_LINE = "§7Random Spawn Egg";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static final Material[] EGGS = {
            Material.ZOMBIE_SPAWN_EGG,
            Material.SKELETON_SPAWN_EGG,
            Material.CAVE_SPIDER_SPAWN_EGG,
            Material.SPIDER_SPAWN_EGG,
            Material.HORSE_SPAWN_EGG,
            Material.COW_SPAWN_EGG,
            Material.SHEEP_SPAWN_EGG,
            Material.PIG_SPAWN_EGG,
            Material.SNIFFER_SPAWN_EGG,
            Material.CAMEL_SPAWN_EGG,
            Material.FROG_SPAWN_EGG,
            Material.GOAT_SPAWN_EGG,
            Material.PANDA_SPAWN_EGG,
            Material.MAGMA_CUBE_SPAWN_EGG,
            Material.CHICKEN_SPAWN_EGG
    };

    @Override
    public String getIdentifier() {
        return "random_spawn_egg";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        Material chosen = EGGS[ThreadLocalRandom.current().nextInt(EGGS.length)];
        ItemStack egg = new ItemStack(chosen, 1);

        // Try to add to inventory; drop if full
        var leftover = player.getInventory().addItem(egg);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }

        String mobName = formatMobName(chosen);
        player.sendMessage(mm.deserialize("<green>You received a <yellow>" + mobName + "</yellow> <green>spawn egg!"));

        // consume one of the Random Spawn Egg item
        assert item != null;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private String formatMobName(Material egg) {
        // Convert e.g. ZOMBIE_SPAWN_EGG -> Zombie
        String raw = egg.name().replace("_SPAWN_EGG", "");
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p.charAt(0)).append(p.substring(1).toLowerCase()).append(' ');
        }
        return sb.toString().trim();
    }
}
