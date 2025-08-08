package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.HashMap;

public class CustomEnchantAnvilClickListener implements Listener {

    @EventHandler
    public void onAnvilResultClick(InventoryClickEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;

        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!(inv instanceof AnvilInventory anvil)) return;

        ItemStack result = event.getCurrentItem();
        ItemStack book = anvil.getItem(1);

        if (result == null || result.getType() == Material.AIR) return;

        event.setCancelled(true);
        event.getView().setCursor(null);

        if (book != null && book.getType() == Material.ENCHANTED_BOOK) {
            if (book.getAmount() <= 1) {
                anvil.setItem(1, null);
            } else {
                book.setAmount(book.getAmount() - 1);
                anvil.setItem(1, book);
            }
        }

        anvil.setItem(0, null);

        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerInventory pinv = player.getInventory();
                HashMap<Integer, ItemStack> leftovers = pinv.addItem(result.clone());

                if (!leftovers.isEmpty()) {
                    for (ItemStack leftover : leftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }

                player.updateInventory();
            }
        }.runTask(CCUtilities.getInstance());
    }
}
