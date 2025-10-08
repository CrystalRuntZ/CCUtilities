package org.celestialcraft.cCUtilities.modules.maparts;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceRegionCommand implements CommandExecutor, TabCompleter {
    private final ResourceRegionManager manager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ResourceRegionCommand(ResourceRegionManager manager) {
        this.manager = manager;
    }

    private boolean isAdmin(CommandSender s) {
        return s.hasPermission("maparts.admin") || s.hasPermission("mapart.admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(mini.deserialize("<gray>/mapres <define|setfill|fill|reset|info|list|remove></gray>"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "define" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(mini.deserialize("<red>Players only.</red>")); return true; }
                if (!isAdmin(p)) { p.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 2) { p.sendMessage(mini.deserialize("<gray>/mapres define <name></gray>")); return true; }

                var pos1 = MapArtSelectionManager.getPos1(p);
                var pos2 = MapArtSelectionManager.getPos2(p);
                if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
                    p.sendMessage(mini.deserialize("<red>Select two corners first.</red>"));
                    return true;
                }
                if (!Objects.equals(pos1.getWorld(), pos2.getWorld())) {
                    p.sendMessage(mini.deserialize("<red>Both corners must be in the same world.</red>"));
                    return true;
                }

                String name = args[1];
                if (manager.exists(name)) { p.sendMessage(mini.deserialize("<red>A resource region with that name exists.</red>")); return true; }

                ResourceRegion rr = getResourceRegion(pos1, pos2, name);
                manager.add(rr);
                MapArtSelectionManager.clear(p);
                p.sendMessage(mini.deserialize("<green>Defined resource region </green><#c1adfe>"+name+"</#c1adfe><green>.</green>"));
                return true;
            }
            case "setfill" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 3) { sender.sendMessage(mini.deserialize("<gray>/mapres setfill <name> <material></gray>")); return true; }
                ResourceRegion rr = manager.get(args[1]);
                if (rr == null) { sender.sendMessage(mini.deserialize("<red>No such region.</red>")); return true; }
                Material mat = Material.matchMaterial(args[2]);
                if (mat == null) { sender.sendMessage(mini.deserialize("<red>Invalid material.</red>")); return true; }
                rr.setFillMaterial(mat);
                manager.save();
                sender.sendMessage(mini.deserialize("<green>Set fill for </green><#c1adfe>"+rr.getName()+"</#c1adfe><green> to </green><#c1adfe>"+mat.name()+"</#c1adfe><green>.</green>"));
                return true;
            }
            case "fill" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 2) { sender.sendMessage(mini.deserialize("<gray>/mapres fill <name></gray>")); return true; }
                ResourceRegion rr = manager.get(args[1]);
                if (rr == null) { sender.sendMessage(mini.deserialize("<red>No such region.</red>")); return true; }
                manager.fillRegion(rr);
                sender.sendMessage(mini.deserialize("<green>Filling region </green><#c1adfe>"+rr.getName()+"</#c1adfe><green>…</green>"));
                return true;
            }
            case "reset" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 2) { sender.sendMessage(mini.deserialize("<gray>/mapres reset <name></gray>")); return true; }
                ResourceRegion rr = manager.get(args[1]);
                if (rr == null) { sender.sendMessage(mini.deserialize("<red>No such region.</red>")); return true; }
                manager.fillRegion(rr);
                sender.sendMessage(mini.deserialize("<green>Reset region </green><#c1adfe>"+rr.getName()+"</#c1adfe><green>…</green>"));
                return true;
            }
            case "info" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 2) { sender.sendMessage(mini.deserialize("<gray>/mapres info <name></gray>")); return true; }
                ResourceRegion rr = manager.get(args[1]);
                if (rr == null) { sender.sendMessage(mini.deserialize("<red>No such region.</red>")); return true; }
                sender.sendMessage(mini.deserialize("<#c1adfe>"+rr.getName()+"</#c1adfe> <gray>World:</gray> "+rr.getWorldName()+ " <gray>Fill:</gray> "+rr.getFillMaterial().name()
                        +" <gray>Bounds:</gray> ["+rr.getMinX()+","+rr.getMinY()+","+rr.getMinZ()+"] → ["+rr.getMaxX()+","+rr.getMaxY()+","+rr.getMaxZ()+"]"));
                return true;
            }
            case "list" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                var names = manager.all().stream().map(ResourceRegion::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
                sender.sendMessage(mini.deserialize("<gray>Resource regions:</gray> <#c1adfe>"+String.join("</#c1adfe><gray>, </gray><#c1adfe>", names)+"</#c1adfe>"));
                return true;
            }
            case "remove" -> {
                if (!isAdmin(sender)) { sender.sendMessage(mini.deserialize("<red>No permission.</red>")); return true; }
                if (args.length < 2) { sender.sendMessage(mini.deserialize("<gray>/mapres remove <name></gray>")); return true; }
                boolean ok = manager.remove(args[1]);
                sender.sendMessage(mini.deserialize(ok ? "<green>Removed region.</green>" : "<red>No such region.</red>"));
                return true;
            }
            default -> {
                sender.sendMessage(mini.deserialize("<red>Unknown subcommand.</red>"));
                return true;
            }
        }
    }

    private static @NotNull ResourceRegion getResourceRegion(Location pos1, Location pos2, String name) {
        World w = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return new ResourceRegion(name, w.getName(), minX, minY, minZ, maxX, maxY, maxZ, Material.RED_CONCRETE);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) return Arrays.asList("define","setfill","fill","reset","info","list","remove");
        if (args.length == 2 && List.of("setfill","fill","reset","info","remove").contains(args[0].toLowerCase(Locale.ROOT))) {
            return manager.all().stream().map(ResourceRegion::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted().collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setfill")) {
            String pre = args[2].toUpperCase(Locale.ROOT);
            return Arrays.stream(Material.values()).map(Material::name)
                    .filter(n -> n.startsWith(pre)).limit(25).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
