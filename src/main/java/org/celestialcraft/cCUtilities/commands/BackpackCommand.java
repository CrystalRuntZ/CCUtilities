package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.customitems.SpiderBackpackItem;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SpiderBackpackItem backpackHelper;

    private static final long CONFIRM_TIMEOUT_MS = 30_000L;
    private final Map<UUID, PendingRestore> pendingRestore = new HashMap<>();

    private static final long UNDO_TIMEOUT_MS = 60_000L;
    private final Map<UUID, PendingUndo> pendingUndo = new HashMap<>();

    public BackpackCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.backpackHelper = new SpiderBackpackItem(plugin);
    }

    private boolean isAllowed(CommandSender sender) {
        if (!(sender instanceof Player p)) return true;
        return p.isOp() || p.hasPermission("celestial.backpack.use");
    }

    private String humanTime(long epochMs) {
        if (epochMs <= 0) return "never";
        return timeFmt.format(Instant.ofEpochMilli(epochMs));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!isAllowed(sender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§eUsage: /backpack <lastopener|preview|restore|info> [...]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir() || !held.hasItemMeta()) {
                player.sendMessage("§cHold a backpack item in your main hand first.");
                return true;
            }

            switch (sub) {
                case "lastopener" -> handleLastOpener(sender, held);
                case "preview" -> handlePreview(sender, held, (args.length >= 2 ? args[1] : "latest"));
                case "restore" -> {
                    if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) handleRestoreConfirm(sender, player);
                    else if (args.length >= 2 && args[1].equalsIgnoreCase("undo")) handleRestoreUndo(sender, player);
                    else handleRestoreStage(sender, player, held, (args.length >= 2 ? args[1] : "latest"));
                }
                case "info" -> handleInfo(sender, held);
                default -> player.sendMessage("§eUnknown subcommand. Use lastopener, preview, restore or info.");
            }
        } else {
            if (args.length < 2) {
                sender.sendMessage("§cConsole must specify a player: /backpack <lastopener|preview|restore|info> <player> [args]");
                return true;
            }
            String playerName = args[1];
            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null) {
                sender.sendMessage("§cPlayer not online: " + playerName);
                return true;
            }
            ItemStack held = target.getInventory().getItemInMainHand();
            if (held.getType().isAir() || !held.hasItemMeta()) {
                sender.sendMessage("§cTarget player is not holding a backpack item.");
                return true;
            }

            switch (sub) {
                case "lastopener" -> handleLastOpener(sender, held);
                case "preview" -> handlePreviewConsole(sender, target, held, (args.length >= 3 ? args[2] : "latest"));
                case "restore" -> {
                    String whichArg = (args.length >= 3 ? args[2] : "latest");
                    int which = parseWhich(whichArg);
                    String previous = (which == -1) ? backpackHelper.restoreLatestBackupWithUndo(held) : backpackHelper.restoreBackupWithUndo(held, which);
                    if (previous != null) {
                        sender.sendMessage("§aBackup restored. Previous primary saved for undo (console cannot undo).");
                    } else {
                        sender.sendMessage("§cNo available backup to restore.");
                    }
                }
                case "info" -> handleInfo(sender, held);
                default -> sender.sendMessage("§eUnknown subcommand. Use lastopener, preview, restore or info.");
            }
        }

        return true;
    }

    private void handleLastOpener(CommandSender sender, ItemStack held) {
        UUID last = backpackHelper.getLastOpener(held);
        long ts = backpackHelper.getLastOpenTimestamp(held);

        if (last == null && ts == 0L) {
            sender.sendMessage("§cNo last-opener information found on this item.");
            return;
        }

        String name = (last == null) ? "unknown" : Bukkit.getOfflinePlayer(last).getName() + " (" + last + ")";
        sender.sendMessage("§aBackpack last opened by: §f" + name);
        sender.sendMessage("§aLast opened at: §f" + (ts == 0 ? "never" : humanTime(ts)));
    }

    private void handlePreview(CommandSender sender, ItemStack held, String whichArg) {
        int which = parseWhich(whichArg);
        if (which == -1) which = 0;
        ItemStack[] items = backpackHelper.getBackupContents(held, which);
        if (items == null || items.length == 0) {
            sender.sendMessage("§cNo items in the chosen backup.");
            return;
        }

        if (whichArg.equalsIgnoreCase("full")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole cannot open inventories. Use preview to list items in chat.");
                return;
            }
            Inventory tmp = Bukkit.createInventory(null, 27, Component.text("Backpack Preview"));
            tmp.setContents(items);
            ((Player) sender).openInventory(tmp);
            sender.sendMessage("§aOpened backpack preview. Closing will not modify the backpack.");
            return;
        }

        sender.sendMessage("§eBackup preview (" + (which == 0 ? "latest" : String.valueOf(which)) + "):");
        int shown = 0;
        for (int i = 0; i < items.length && shown < 20; i++) {
            ItemStack it = items[i];
            if (it == null) continue;

            String name = readableName(it);
            sender.sendMessage(" §7Slot " + i + ": §f" + name + " x" + it.getAmount());
            shown++;
        }
        if (shown == 0) sender.sendMessage(" §7(empty)");
        else if (items.length > shown) sender.sendMessage(" §7... (truncated)");
    }

    private void handlePreviewConsole(CommandSender sender, Player target, ItemStack held, String whichArg) {
        int which = parseWhich(whichArg);
        if (which == -1) which = 0;
        ItemStack[] items = backpackHelper.getBackupContents(held, which);
        if (items == null || items.length == 0) {
            sender.sendMessage("No items in the chosen backup for " + target.getName() + ".");
            return;
        }
        sender.sendMessage("Backup preview for " + target.getName() + " (" + (which == 0 ? "latest" : String.valueOf(which)) + "):");
        int shown = 0;
        for (int i = 0; i < items.length && shown < 100; i++) {
            ItemStack it = items[i];
            if (it == null) continue;

            String name = readableName(it);
            sender.sendMessage(" Slot " + i + ": " + name + " x" + it.getAmount());
            shown++;
        }
    }

    private void handleRestoreStage(CommandSender sender, Player p, ItemStack held, String whichArg) {
        int which = parseWhich(whichArg);
        boolean has;
        ItemStack[] c;
        if (which == -1) {
            c = backpackHelper.getBackupContents(held, 0);
        } else {
            c = backpackHelper.getBackupContents(held, which);
        }
        has = c != null && c.length > 0 && Arrays.stream(c).anyMatch(Objects::nonNull);
        if (!has) {
            sender.sendMessage("§cNo available backup to restore for that selector.");
            return;
        }

        String id = backpackHelper.getBackpackId(held);
        PendingRestore pr = new PendingRestore(id, which, System.currentTimeMillis() + CONFIRM_TIMEOUT_MS);
        pendingRestore.put(p.getUniqueId(), pr);

        sender.sendMessage("§eRestore staged. Run §6/backpack restore confirm§e within 30 seconds while holding the same backpack to proceed.");
    }

    private void handleRestoreConfirm(CommandSender sender, Player p) {
        PendingRestore pr = pendingRestore.get(p.getUniqueId());
        if (pr == null) {
            sender.sendMessage("§cNo pending restore found. Stage one with /backpack restore <1|2|latest> first.");
            return;
        }
        if (System.currentTimeMillis() > pr.expiresAt) {
            pendingRestore.remove(p.getUniqueId());
            sender.sendMessage("§cYour pending restore has expired. Stage again to restore.");
            return;
        }

        ItemStack held = p.getInventory().getItemInMainHand();
        String currentId = backpackHelper.getBackpackId(held);
        if (currentId == null || !currentId.equals(pr.backpackId)) {
            pendingRestore.remove(p.getUniqueId());
            sender.sendMessage("§cBackpack mismatch. Hold the same backpack item you staged the restore for and try again.");
            return;
        }

        String previousYaml;
        if (pr.which == -1) previousYaml = backpackHelper.restoreLatestBackupWithUndo(held);
        else previousYaml = backpackHelper.restoreBackupWithUndo(held, pr.which);

        pendingRestore.remove(p.getUniqueId());

        if (previousYaml != null) {
            PendingUndo undo = new PendingUndo(backpackHelper.getBackpackId(held), previousYaml, System.currentTimeMillis() + UNDO_TIMEOUT_MS);
            pendingUndo.put(p.getUniqueId(), undo);
            sender.sendMessage("§aRestore completed successfully. You may undo with §6/backpack restore undo§a within 60 seconds.");
        } else {
            sender.sendMessage("§cRestore failed (no data available).");
        }
    }

    private void handleRestoreUndo(CommandSender sender, Player p) {
        PendingUndo undo = pendingUndo.get(p.getUniqueId());
        if (undo == null) {
            sender.sendMessage("§cNo undo available.");
            return;
        }
        if (System.currentTimeMillis() > undo.expiresAt) {
            pendingUndo.remove(p.getUniqueId());
            sender.sendMessage("§cUndo window expired.");
            return;
        }

        ItemStack held = p.getInventory().getItemInMainHand();
        String id = backpackHelper.getBackpackId(held);
        if (id == null || !id.equals(undo.backpackId)) {
            pendingUndo.remove(p.getUniqueId());
            sender.sendMessage("§cBackpack mismatch. Hold the same backpack item and try again.");
            return;
        }

        boolean ok = false;
        if (held.hasItemMeta()) {
            ItemMeta meta = held.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, "spider_backpack_data"), PersistentDataType.STRING, undo.previousYaml);
            held.setItemMeta(meta);
            ok = true;
        }

        pendingUndo.remove(p.getUniqueId());
        if (ok) sender.sendMessage("§aUndo successful — backpack restored to pre-restore state.");
        else sender.sendMessage("§cUndo failed.");
    }

    private void handleInfo(CommandSender sender, ItemStack held) {
        long bak1 = backpackHelper.getBackupTimestamp(held, 1);
        long bak2 = backpackHelper.getBackupTimestamp(held, 2);
        sender.sendMessage("§eBackpack backups:");
        sender.sendMessage(" §7Latest (bak1): §f" + (bak1 == 0 ? "missing" : humanTime(bak1)));
        sender.sendMessage(" §7Older (bak2):  §f" + (bak2 == 0 ? "missing" : humanTime(bak2)));
    }

    private int parseWhich(String arg) {
        if (arg == null) return -1;
        String a = arg.toLowerCase(Locale.ROOT);
        return switch (a) {
            case "1" -> 1;
            case "2" -> 2;
            default -> -1;
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("lastopener","preview","restore","info")
                    .filter(x -> x.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("preview") || sub.equals("restore")) {
                return Stream.of("latest","1","2","full")
                        .filter(x -> x.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            if (! (sender instanceof Player)) {
                String prefix = args[1];
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private static class PendingRestore {
        final String backpackId;
        final int which;
        final long expiresAt;
        PendingRestore(String id, int which, long expiresAt) { this.backpackId = id; this.which = which; this.expiresAt = expiresAt; }
    }

    private static class PendingUndo {
        final String backpackId;
        final String previousYaml;
        final long expiresAt;
        PendingUndo(String id, String previousYaml, long expiresAt) { this.backpackId = id; this.previousYaml = previousYaml; this.expiresAt = expiresAt; }
    }

    // Helper to safely get item display name as String
    private static String readableName(ItemStack it) {
        if (it == null) return "null";
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            Component disp = im.displayName();
            if (disp != null && !disp.equals(Component.empty())) {
                return LegacyComponentSerializer.legacySection().serialize(disp);
            }
        }
        return it.getType().toString();
    }
}
