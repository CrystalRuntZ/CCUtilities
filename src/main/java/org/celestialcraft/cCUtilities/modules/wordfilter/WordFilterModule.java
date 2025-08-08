package org.celestialcraft.cCUtilities.modules.wordfilter;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordFilterModule implements Listener, Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;
    private final Set<String> filteredWords = new HashSet<>();
    private String notifyPermission = "celestialutilities.wordfilter.notify";
    private String bypassPermission = "celestialutilities.wordfilter.bypass";
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public WordFilterModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        var section = plugin.getConfig().getConfigurationSection("word-filter");
        if (section == null) return;

        enabled = section.getBoolean("enabled", false);
        filteredWords.clear();

        List<String> words = section.getStringList("filtered-words");
        for (String word : words) {
            filteredWords.add(word.toLowerCase());
        }

        notifyPermission = section.getString("notify-permission") != null ? section.getString("notify-permission") : notifyPermission;
        bypassPermission = section.getString("bypass-permission") != null ? section.getString("bypass-permission") : bypassPermission;

        if (enabled) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("WordFilter module enabled.");
        }
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLogger().info("WordFilter module disabled.");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "wordfilter";
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!ModuleManager.isEnabled("wordfilter")) return;
        if (event.getPlayer().hasPermission(bypassPermission)) return;

        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).toLowerCase();
        String matchedWord = filteredWords.stream().filter(message::contains).findFirst().orElse(null);
        if (matchedWord == null) return;

        event.setCancelled(true);

        String staffMsg = MessageConfig.get("wordfilter.notify-message")
                .replace("%player%", event.getPlayer().getName())
                .replace("%WORD%", matchedWord);

        String playerMsg = MessageConfig.get("wordfilter.blocked-message")
                .replace("%WORD%", matchedWord);

        Component staffComponent = serializer.deserialize(staffMsg);
        Component playerComponent = serializer.deserialize(playerMsg);

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(notifyPermission))
                .forEach(p -> p.sendMessage(staffComponent));

        event.getPlayer().sendMessage(playerComponent);
    }
}
