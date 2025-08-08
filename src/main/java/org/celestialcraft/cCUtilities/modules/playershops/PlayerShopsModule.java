package org.celestialcraft.cCUtilities.modules.playershops;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.playershops.data.ShopDataManager;

public class PlayerShopsModule implements Module {

    private final JavaPlugin plugin;
    private Material selectionItem;
    private boolean enabled = false;

    public PlayerShopsModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        loadConfig();
        ShopDataManager.load(plugin);
        // Listeners and commands registered centrally
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "playershops";
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        String itemName = config.getString("playershops.selection-item", "STONE_HOE");
        Material matched = Material.matchMaterial(itemName.toUpperCase());
        this.selectionItem = matched != null ? matched : Material.STONE_HOE;
    }

    public Material getSelectionItem() {
        return enabled ? selectionItem : null;
    }
}
