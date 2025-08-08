package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MoltenCorePickaxeItem implements CustomItem {

    private static final String LORE_LINE = "§7Molten Core Pickaxe";
    private static final int OBSIDIAN_PER_INGOT = 4;
    private final Map<UUID, Integer> minedCount = new HashMap<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "molten_core_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(line -> serializer.serialize(line).contains(LORE_LINE));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.OBSIDIAN) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        UUID uuid = player.getUniqueId();
        int count = minedCount.getOrDefault(uuid, 0);

        if (count >= OBSIDIAN_PER_INGOT) {
            if (removeItem(player.getInventory())) {
                minedCount.put(uuid, 0);
            } else {
                player.sendMessage("§cYou need iron ingots to fuel the Molten Core Pickaxe!");
                event.setCancelled(true);
                return;
            }
        }

        block.setType(Material.AIR);
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.OBSIDIAN));
        minedCount.put(uuid, count + 1);
        event.setCancelled(true);
    }

    private boolean removeItem(Inventory inventory) {
        int remaining = 1;

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() != Material.IRON_INGOT) continue;

            int stackAmount = item.getAmount();
            if (stackAmount > remaining) {
                item.setAmount(stackAmount - remaining);
                return true;
            } else {
                inventory.removeItem(item);
                remaining -= stackAmount;
                if (remaining <= 0) return true;
            }
        }

        return false;
    }
}
