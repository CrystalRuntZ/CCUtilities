package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.entitymanager.ChunkEntityTracker;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityLimitManager;
import org.celestialcraft.cCUtilities.modules.entitymanager.EntityManagerModule;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class EntityManagerCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final EntityManagerModule entityManagerModule;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public EntityManagerCommand(JavaPlugin plugin, EntityManagerModule entityManagerModule) {
        this.plugin = plugin;
        this.entityManagerModule = entityManagerModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!ModuleManager.isEnabled("entitymanager")) return true;

        if (args.length == 0) {
            sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.usage").replace("%label%", label)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.player-only")));
                    return true;
                }

                if (!sender.hasPermission("entitymanager.list")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.no-permission")));
                    return true;
                }

                List<Map.Entry<Chunk, Integer>> topChunks = ChunkEntityTracker.getTopChunks(10);
                if (topChunks.isEmpty()) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.no-entity-chunks")));
                    return true;
                }

                sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.chunk-header")));
                for (Map.Entry<Chunk, Integer> entry : topChunks) {
                    Chunk chunk = entry.getKey();
                    int count = entry.getValue();
                    World world = chunk.getWorld();
                    Location loc = chunk.getBlock(8, 64, 8).getLocation();

                    String hover = "Click to teleport to " + world.getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
                    Component text = Component.text("  → [" + count + " entities] " + world.getName() + " @ " + chunk.getX() + ", " + chunk.getZ())
                            .color(NamedTextColor.AQUA)
                            .hoverEvent(HoverEvent.showText(Component.text(hover)))
                            .clickEvent(ClickEvent.runCommand("/tp @s " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
                    sender.sendMessage(text);
                }
                break;

            case "limit":
                if (!sender.hasPermission("entitymanager.limit")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.no-permission")));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.usage-limit").replace("%label%", label)));
                    return true;
                }

                String mob = args[1].toUpperCase();
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.invalid-number")));
                    return true;
                }

                EntityLimitManager.setMobLimit(mob, amount);
                sender.sendMessage(mini.deserialize(
                        MessageConfig.get("entitymanager.mob-limit-updated")
                                .replace("%mob%", mob)
                                .replace("%amount%", String.valueOf(amount))
                ));
                break;

            case "limitblock":
                if (!sender.hasPermission("entitymanager.limit")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.no-permission")));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.usage-limitblock").replace("%label%", label)));
                    return true;
                }

                String blockName = args[1].toUpperCase();
                int blockAmount;
                try {
                    blockAmount = Integer.parseInt(args[2]);
                    if (blockAmount < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.invalid-number")));
                    return true;
                }

                Material material = Material.matchMaterial(blockName);
                if (material == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.invalid-block")));
                    return true;
                }

                EntityLimitManager.setBlockLimit(material, blockAmount);
                EntityLimitManager.saveAllLimitsToConfig(plugin.getConfig(), plugin);

                sender.sendMessage(mini.deserialize(
                        MessageConfig.get("entitymanager.block-limit-updated")
                                .replace("%block%", blockName)
                                .replace("%amount%", String.valueOf(blockAmount))
                ));
                break;

            case "reload":
                if (!sender.hasPermission("entitymanager.reload")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.no-permission")));
                    return true;
                }

                entityManagerModule.reload();
                sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.reloaded")));
                break;

            case "viewlimits":
                sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.view-header-mob")));
                for (Map.Entry<String, Integer> entry : EntityLimitManager.getAllMobLimits().entrySet()) {
                    sender.sendMessage(mini.deserialize("  <gray>" + entry.getKey() + "</gray>: <white>" + entry.getValue() + "</white>"));
                }

                sender.sendMessage(mini.deserialize(MessageConfig.get("entitymanager.view-header-block")));
                for (Map.Entry<Material, Integer> entry : EntityLimitManager.getAllBlockLimits().entrySet()) {
                    sender.sendMessage(mini.deserialize("  <gray>" + entry.getKey() + "</gray>: <white>" + entry.getValue() + "</white>"));
                }
                break;

            default:
                sender.sendMessage(mini.deserialize(
                        MessageConfig.get("entitymanager.unknown-subcommand").replace("%arg%", args[0])
                ));
                break;
        }

        return true;
    }
}
