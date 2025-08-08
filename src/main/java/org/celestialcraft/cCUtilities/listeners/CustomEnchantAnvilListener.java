package org.celestialcraft.cCUtilities.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.*;
import java.util.regex.Pattern;

public class CustomEnchantAnvilListener implements Listener {

    private static final Pattern VANILLA_ENCHANT_PATTERN = Pattern.compile("^[A-Za-z ]+ [IVXLCDM]+$");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilLoreApply(PrepareAnvilEvent event) {
        if (!ModuleManager.isEnabled("customenchants")) return;
        AnvilInventory inv = event.getInventory();
        ItemStack baseItem = inv.getItem(0);
        ItemStack bookItem = inv.getItem(1);

        if (baseItem == null || bookItem == null) return;
        if (bookItem.getType() != Material.ENCHANTED_BOOK) return;

        List<String> loreToAppend = new ArrayList<>();
        for (CustomEnchant enchant : CustomEnchantRegistry.getAll()) {
            if (!enchant.appliesTo(baseItem)) continue;
            if (enchant.hasEnchant(baseItem)) continue;
            if (!enchant.hasEnchant(bookItem)) continue;

            String rawLore = ChatColor.translateAlternateColorCodes('&', enchant.getLoreLine());
            loreToAppend.add(rawLore);
        }

        if (loreToAppend.isEmpty()) return;

        ItemStack mergedResult = event.getResult();
        if (mergedResult == null || mergedResult.getType() == Material.AIR) return;

        ItemStack result = mergedResult.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        List<String> existingLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean changed = false;

        int insertIndex = 0;
        for (int i = 0; i < existingLore.size(); i++) {
            String line = ChatColor.stripColor(existingLore.get(i));
            if (VANILLA_ENCHANT_PATTERN.matcher(line).matches()) {
                insertIndex = i + 1;
            } else {
                break;
            }
        }

        for (int i = loreToAppend.size() - 1; i >= 0; i--) {
            String newLine = loreToAppend.get(i);
            if (!existingLore.contains(newLine)) {
                existingLore.add(insertIndex, newLine);
                changed = true;
            }
        }

        if (!changed) return;

        meta.setLore(existingLore);
        result.setItemMeta(meta);
        event.setResult(result);
    }
}
