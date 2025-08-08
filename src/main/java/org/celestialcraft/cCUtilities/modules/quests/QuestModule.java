package org.celestialcraft.cCUtilities.modules.quests;

import org.bukkit.configuration.file.YamlConfiguration;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.config.QuestConfig;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestCooldowns;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestStorage;

import java.io.File;

public class QuestModule implements Module {

    private final CCUtilities plugin;
    private boolean enabled = false;

    public QuestModule(CCUtilities plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        File configFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!configFile.exists()) {
            plugin.saveResource("quests.yml", false);
        }

        YamlConfiguration questConfig = YamlConfiguration.loadConfiguration(configFile);
        QuestConfig.load(questConfig);

        QuestStorage.initialize(plugin);
        QuestCooldowns.initialize(plugin);

        plugin.getLogger().info("Quests module enabled.");
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        QuestStorage.saveAll();
        QuestCooldowns.save();

        plugin.getLogger().info("Quests module disabled.");
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "quests";
    }

    public void reload() {
        if (!enabled) return;
        disable();
        enable();
    }
}
