package org.celestialcraft.cCUtilities.modules.joinitem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class JoinItemModule implements Listener, Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public JoinItemModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        return "joinitem";
    }

    private ItemStack createJoinItem(FileConfiguration config, String playerName) {
        Material material = Material.matchMaterial(config.getString("join-item.material", "DIAMOND"));
        if (material == null) material = Material.DIAMOND;

        String nameRaw = config.getString("join-item.name", "&bWelcome Gift");
        List<String> loreRaw = config.getStringList("join-item.lore");

        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));

        List<String> replacedLore = loreRaw.stream()
                .map(line -> line.replace("%PLAYER_NAME%", playerName).replace("%DATE%", formattedDate))
                .toList();

        Component name = serializer.deserialize(nameRaw).decoration(TextDecoration.ITALIC, false);
        List<Component> lore = replacedLore.stream()
                .map(line -> serializer.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            if (config.contains("join-item.custom-model-data")) {
                meta.setCustomModelData(config.getInt("join-item.custom-model-data"));
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            ItemStack item = createJoinItem(plugin.getConfig(), player.getName());
            int slot = plugin.getConfig().getInt("join-item.slot", 0);

            if (player.getInventory().getItem(slot) == null) {
                player.getInventory().setItem(slot, item);
            } else {
                player.getInventory().addItem(item);
            }
        }
    }
}
