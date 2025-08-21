package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundle;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundleStorage;
import org.celestialcraft.cCUtilities.modules.quests.config.QuestConfig;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestCooldowns;
import org.celestialcraft.cCUtilities.modules.quests.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class QuestsCommand implements CommandExecutor {
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!ModuleManager.isEnabled("quests")) return true;
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (!sender.hasPermission("quests.give")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.usage-give")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-not-found")));
                    return true;
                }

                String questId = args[2];
                var template = QuestConfig.getTemplate(questId);
                if (template == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.invalid-id").replace("%id%", questId)));
                    return true;
                }

                if (QuestManager.hasQuestOfType(target, template.type())) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.already-has-type").replace("%player%", target.getName())));
                    return true;
                }

                Quest quest = new Quest(
                        UUID.randomUUID(),
                        questId,
                        template.type(),
                        template.target(),
                        template.expiration(),
                        template.claimWindow(),
                        template.targetItem()
                );

                target.getInventory().addItem(QuestItemFactory.createQuestItem(quest));
                QuestManager.addQuest(target, quest);

                sender.sendMessage(mini.deserialize(MessageConfig.get("quests.give-success")
                        .replace("%player%", target.getName())
                        .replace("%quest%", template.display())));
                return true;
            }

            case "weekly" -> {
                // Players only
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(MessageConfig.get("quests.players-only")));
                    return true;
                }

                // Permission gate
                if (!player.hasPermission("quests.weekly")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }

                // Optional admin bypass: quests.weekly.bypass
                if (player.hasPermission("quests.weekly.bypass")) {
                    // ===== existing grant logic (bypasses cooldown) =====
                    List<Quest> generated = WeeklyQuestGenerator.generateWeeklyQuests(player.getUniqueId());
                    long oneWeekMs = 7L * 24 * 60 * 60 * 1000;
                    long expiresAtEpochSeconds = (System.currentTimeMillis() + oneWeekMs) / 1000L;

                    WeeklyBundle bundle = new WeeklyBundle(
                            java.util.UUID.randomUUID(),
                            player.getUniqueId(),
                            expiresAtEpochSeconds,
                            generated
                    );

                    WeeklyBundleStorage storage = new WeeklyBundleStorage(CCUtilities.getInstance());
                    storage.save(bundle);

                    ItemStack paper = WeeklyQuestItemFactory.build(CCUtilities.getInstance(), bundle);
                    paper.editMeta(meta -> meta.displayName(
                            MiniMessage.miniMessage()
                                    .deserialize("<#c1adfe>" + player.getName() + "'s Weekly Quest</#c1adfe>")
                                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                    ));
                    // Delegate lore rendering to LoreUtils (consistent style)
                    LoreUtils.updateLoreMultiple(paper, generated);
                    player.getInventory().addItem(paper);

                    player.sendMessage(MiniMessage.miniMessage().deserialize(MessageConfig.get("quests.weekly.granted")));
                    return true;
                }

                // Cooldown via WeeklyClaimGate (PDC-backed)
                WeeklyClaimGate gate = new WeeklyClaimGate(CCUtilities.getInstance());
                long now = System.currentTimeMillis();

                if (!gate.canClaim(player, now)) {
                    long rem = gate.millisRemaining(player, now);
                    String pretty = WeeklyClaimGate.formatRemaining(rem);
                    player.sendMessage(
                            MiniMessage.miniMessage().deserialize(
                                    MessageConfig.get("quests.weekly.cooldown")
                                            .replace("%remaining%", pretty)
                            )
                    );
                    return true;
                }

                // ===== cooldown satisfied -> grant and record =====
                List<Quest> generated = WeeklyQuestGenerator.generateWeeklyQuests(player.getUniqueId());
                long oneWeekMs = 7L * 24 * 60 * 60 * 1000;
                long expiresAtEpochSeconds = (System.currentTimeMillis() + oneWeekMs) / 1000L;

                WeeklyBundle bundle = new WeeklyBundle(
                        java.util.UUID.randomUUID(),
                        player.getUniqueId(),
                        expiresAtEpochSeconds,
                        generated
                );

                WeeklyBundleStorage storage = new WeeklyBundleStorage(CCUtilities.getInstance());
                storage.save(bundle);

                ItemStack paper = WeeklyQuestItemFactory.build(CCUtilities.getInstance(), bundle);
                paper.editMeta(meta -> meta.displayName(
                        MiniMessage.miniMessage()
                                .deserialize("<#c1adfe>" + player.getName() + "'s Weekly Quest</#c1adfe>")
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                ));
                // Delegate lore rendering to LoreUtils (consistent style)
                LoreUtils.updateLoreMultiple(paper, generated);
                player.getInventory().addItem(paper);

                // record claim and notify
                gate.recordClaim(player, now);
                player.sendMessage(MiniMessage.miniMessage().deserialize(MessageConfig.get("quests.weekly.granted")));
                return true;
            }


            case "expiry" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }

                var item = player.getInventory().getItemInMainHand();
                if (!item.hasItemMeta()) {
                    player.sendMessage(mini.deserialize("<red>Hold the quest item or weekly paper in your main hand.</red>"));
                    return true;
                }

                // Weekly paper?
                var plugin = CCUtilities.getInstance();
                var bundleId = WeeklyQuestItemFactory.getBundleId(plugin, item);
                if (bundleId != null) {
                    WeeklyBundleStorage storage = new WeeklyBundleStorage(plugin);
                    WeeklyBundle bundle = storage.load(bundleId);
                    if (bundle == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t read this weekly quest bundle.</red>"));
                        return true;
                    }
                    long nowSec = Instant.now().getEpochSecond();
                    long remaining = bundle.getExpiresAtEpochSeconds() - nowSec;
                    if (remaining <= 0) {
                        player.sendMessage(mini.deserialize("<yellow>Your weekly quest paper has <red>expired</red>.</yellow>"));
                    } else {
                        player.sendMessage(mini.deserialize("<green>Weekly quests expire in</green> <aqua>" + formatDuration(remaining) + "</aqua>."));
                    }
                    return true;
                }

                // Single quest item?
                var questUuid = LoreUtils.getQuestId(item);
                if (questUuid != null) {
                    List<Quest> quests = QuestManager.getQuests(player);
                    Quest match = null;
                    for (Quest q : quests) {
                        if (q.getId().equals(questUuid)) { match = q; break; }
                    }
                    if (match == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t find the quest data for this item.</red>"));
                        return true;
                    }

                    long endMs = match.getStartTime() + (match.getExpirationSeconds() * 1000L);
                    long remainingSec = (endMs - System.currentTimeMillis()) / 1000L;
                    if (remainingSec <= 0) {
                        player.sendMessage(mini.deserialize("<yellow>This quest has <red>expired</red>.</yellow>"));
                    } else {
                        player.sendMessage(mini.deserialize("<green>This quest expires in</green> <aqua>" + formatDuration(remainingSec) + "</aqua>."));
                    }
                    return true;
                }

                player.sendMessage(mini.deserialize("<red>Hold a quest item or the weekly quest paper in your main hand.</red>"));
                return true;
            }

            // -------- NEW ADMIN / TEST UTILITIES --------

            case "paper" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }
                if (!sender.hasPermission("quests.admin")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }
                Player target = (args.length >= 2) ? Bukkit.getPlayer(args[1]) : (Player) sender;
                if (target == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-not-found")));
                    return true;
                }
                WeeklyBundleStorage storage = new WeeklyBundleStorage(CCUtilities.getInstance());
                long now = Instant.now().getEpochSecond();
                WeeklyBundle bundle = storage.findActiveFor(target.getUniqueId(), now);
                if (bundle == null || bundle.isExpired(now)) {
                    sender.sendMessage(mini.deserialize("<red>No active weekly bundle for that player.</red>"));
                    return true;
                }
                target.getInventory().addItem(WeeklyQuestItemFactory.build(CCUtilities.getInstance(), bundle));
                sender.sendMessage(mini.deserialize("<green>Reissued weekly quest paper to</green> <aqua>" + target.getName() + "</aqua>."));
                return true;
            }

            case "complete" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }
                if (!sender.hasPermission("quests.admin")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }

                var plugin = CCUtilities.getInstance();
                var item = player.getInventory().getItemInMainHand();
                if (!item.hasItemMeta()) {
                    player.sendMessage(mini.deserialize("<red>Hold a quest item or the weekly quest paper in your main hand.</red>"));
                    return true;
                }

                long nowMs = System.currentTimeMillis();

                // A) Weekly paper: time-warp ALL quests in the bundle so they are claimable via /quests claim
                var bundleId = WeeklyQuestItemFactory.getBundleId(plugin, item);
                if (bundleId != null) {
                    WeeklyBundleStorage storage = new WeeklyBundleStorage(plugin);
                    WeeklyBundle bundle = storage.load(bundleId);
                    if (bundle == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t read this weekly quest bundle.</red>"));
                        return true;
                    }

                    // Build a new quest list with warped times + completed progress
                    List<Quest> warped = new java.util.ArrayList<>(bundle.getQuests().size());
                    for (Quest q : bundle.getQuests()) {
                        Quest q2 = timeWarpToClaimable(q, nowMs);
                        warped.add(q2);
                    }
                    bundle.setQuests(warped);
                    storage.save(bundle);

                    // Mirror into QuestManager (replace matching quests by id)
                    List<Quest> active = QuestManager.getQuests(player);
                    for (int i = 0; i < active.size(); i++) {
                        Quest aq = active.get(i);
                        for (Quest bq : warped) {
                            if (aq.getId().equals(bq.getId())) {
                                active.set(i, bq);
                                break;
                            }
                        }
                    }

                    // Update paper lore to reflect completion
                    WeeklyQuestItemFactory.syncLore(plugin, item, bundle);

                    player.sendMessage(mini.deserialize("<green>Marked weekly quests complete and made them claimable. Use</green> <aqua>/quests claim</aqua>."));
                    return true;
                }

                // B) Single quest item: time-warp just this quest
                var questUuid = LoreUtils.getQuestId(item);
                if (questUuid == null) {
                    player.sendMessage(mini.deserialize("<red>This item is not recognized as a quest.</red>"));
                    return true;
                }

                List<Quest> quests = QuestManager.getQuests(player);
                boolean replaced = false;

                for (int i = 0; i < quests.size(); i++) {
                    Quest q = quests.get(i);
                    if (q.getId().equals(questUuid)) {
                        Quest q2 = timeWarpToClaimable(q, nowMs);
                        quests.set(i, q2);
                        LoreUtils.updateLore(item, q2);
                        replaced = true;
                        break;
                    }
                }

                if (!replaced) {
                    player.sendMessage(mini.deserialize("<red>Couldn’t find quest data for this item.</red>"));
                    return true;
                }

                player.sendMessage(mini.deserialize("<green>Marked the held quest complete and made it claimable. Use</green> <aqua>/quests claim</aqua>."));
                return true;
            }

            case "resetweekly" -> {
                if (!sender.hasPermission("quests.admin")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(mini.deserialize("<red>Usage:</red> /quests resetweekly <player>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-not-found")));
                    return true;
                }

                // Purge all stored bundles for this player
                int purged = QuestProgress.purgeOwner(target.getUniqueId());

                // Let them claim immediately again
                QuestCooldowns.setLastWeeklyClaim(target.getUniqueId(), 0L);
                QuestCooldowns.save();

                // Remove any stale weekly items from their inventory and sync lore on valid ones
                int removed = QuestProgress.removeInvalidWeeklyItems(target);

                sender.sendMessage(mini.deserialize(
                        "<yellow>Reset weekly state for</yellow> <aqua>" + target.getName() +
                                "</aqua> <gray>(purged " + purged + " bundles, removed " + removed + " stale items)</gray>."
                ));
                return true;
            }

            case "progress" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }
                if (!sender.hasPermission("quests.admin")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }
                Player target = (args.length >= 2) ? Bukkit.getPlayer(args[1]) : (Player) sender;
                if (target == null) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-not-found")));
                    return true;
                }

                WeeklyBundleStorage storage = new WeeklyBundleStorage(CCUtilities.getInstance());
                long now = Instant.now().getEpochSecond();
                WeeklyBundle bundle = storage.findActiveFor(target.getUniqueId(), now);

                if (bundle != null && !bundle.isExpired(now)) {
                    sender.sendMessage(mini.deserialize("<gray>—</gray> <gold>Weekly Bundle</gold> <gray>—</gray>"));
                    for (Quest q : bundle.getQuests()) {
                        sender.sendMessage(mini.deserialize(formatQuestLine(q)));
                    }
                    return true;
                }

                // Fallback to legacy quests visible in QuestManager
                List<Quest> quests = QuestManager.getQuests(target);
                if (quests.isEmpty()) {
                    sender.sendMessage(mini.deserialize("<gray>No quests found for that player.</gray>"));
                    return true;
                }
                sender.sendMessage(mini.deserialize("<gray>—</gray> <gold>Legacy Quests</gold> <gray>—</gray>"));
                for (Quest q : quests) {
                    sender.sendMessage(mini.deserialize(formatQuestLine(q)));
                }
                return true;
            }

            case "forceclaim" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }
                if (!sender.hasPermission("quests.admin")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }

                var item = player.getInventory().getItemInMainHand();
                if (!item.hasItemMeta()) {
                    player.sendMessage(mini.deserialize("<red>Hold a quest item or the weekly paper in your main hand.</red>"));
                    return true;
                }

                var plugin = CCUtilities.getInstance();

                // A) Weekly paper: force-claim ALL quests in the bundle
                var bundleId = WeeklyQuestItemFactory.getBundleId(plugin, item);
                if (bundleId != null) {
                    WeeklyBundleStorage storage = new WeeklyBundleStorage(plugin);
                    WeeklyBundle bundle = storage.load(bundleId);
                    if (bundle == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t read this weekly quest bundle.</red>"));
                        return true;
                    }

                    int paid = 0;

                    // Ensure complete, then pay rewards for each quest
                    for (Quest q : bundle.getQuests()) {
                        if (q.getProgress() < q.getTarget()) {
                            q.setProgress(q.getTarget());
                        }
                        runRewards(player, q);
                        paid++;
                    }

                    // Save and sync lore for the paper
                    storage.save(bundle);
                    WeeklyQuestItemFactory.syncLore(plugin, item, bundle);

                    // Mirror to QuestManager so it reflects completion in memory
                    List<Quest> active = QuestManager.getQuests(player);
                    for (Quest bq : bundle.getQuests()) {
                        for (Quest aq : active) {
                            if (aq.getId().equals(bq.getId())) {
                                aq.setProgress(aq.getTarget());
                            }
                        }
                    }

                    player.sendMessage(mini.deserialize("<green>Force-claimed</green> <aqua>" + paid + "</aqua> <green>quests from your weekly paper.</green>"));
                    return true;
                }

                // B) Single quest item: force-claim just this quest
                var questUuid = LoreUtils.getQuestId(item);
                if (questUuid == null) {
                    player.sendMessage(mini.deserialize("<red>This item is not recognized as a quest.</red>"));
                    return true;
                }

                List<Quest> quests = QuestManager.getQuests(player);
                Quest match = null;
                for (Quest q : quests) {
                    if (q.getId().equals(questUuid)) { match = q; break; }
                }
                if (match == null) {
                    player.sendMessage(mini.deserialize("<red>Couldn’t find quest data for this item.</red>"));
                    return true;
                }

                // Mark complete and pay immediately
                if (match.getProgress() < match.getTarget()) {
                    match.setProgress(match.getTarget());
                }
                runRewards(player, match);
                LoreUtils.updateLore(item, match);

                player.sendMessage(mini.deserialize("<green>Force-claimed the held quest.</green>"));
                return true;
            }


            // --------------------------------------------

            case "list" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }

                List<Quest> quests = QuestManager.getQuests(player);
                if (quests.isEmpty()) {
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.no-active")));
                    return true;
                }

                player.sendMessage(mini.deserialize(MessageConfig.get("quests.list-header")));
                for (Quest q : quests) {
                    String status = q.isComplete() ? MessageConfig.get("quests.status-complete") : MessageConfig.get("quests.status-incomplete");
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.list-entry")
                            .replace("%status%", status)
                            .replace("%id%", q.getQuestId())
                            .replace("%progress%", String.valueOf(q.getProgress()))
                            .replace("%target%", String.valueOf(q.getTarget()))));
                }
                return true;
            }

            case "claim" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }

                var plugin = CCUtilities.getInstance();
                var hand = player.getInventory().getItemInMainHand();

                // 1) If holding weekly paper: claim the *whole* weekly if all subquests complete
                var bundleId = hand.hasItemMeta() ? WeeklyQuestItemFactory.getBundleId(plugin, hand) : null;
                if (bundleId != null) {
                    WeeklyBundleStorage storage = new WeeklyBundleStorage(plugin);
                    WeeklyBundle bundle = storage.load(bundleId);
                    if (bundle == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t read this weekly quest bundle.</red>"));
                        return true;
                    }
                    // complete when all subquests are done
                    boolean allDone = true;
                    for (Quest q : bundle.getQuests()) {
                        if (!q.isComplete()) { allDone = false; break; }
                    }
                    if (!allDone) {
                        player.sendMessage(mini.deserialize("<yellow>Your weekly quest isn’t complete yet.</yellow>"));
                        return true;
                    }

                    // run reward group "weekly"
                    for (String raw : QuestConfig.getRewardGroup("weekly")) {
                        String cmd = raw.replace("%player%", player.getName()).replace("{player}", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }

                    // remove paper from hand
                    if (hand.getAmount() <= 1) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        hand.setAmount(hand.getAmount() - 1);
                        player.getInventory().setItemInMainHand(hand);
                    }

                    // purge the bundle so it can't be re-claimed
                    storage.deleteBundle(bundle.getBundleId());

                    player.sendMessage(mini.deserialize("<green>Claimed your weekly quest reward!</green>"));
                    return true;
                }

                // 2) If holding a single quest item: claim that quest immediately if complete
                var heldQuestId = hand.hasItemMeta() ? LoreUtils.getQuestId(hand) : null;
                if (heldQuestId != null) {
                    List<Quest> quests = QuestManager.getQuests(player);
                    Quest match = null;
                    for (Quest q : quests) {
                        if (q.getId().equals(heldQuestId)) { match = q; break; }
                    }
                    if (match == null) {
                        player.sendMessage(mini.deserialize("<red>Couldn’t find quest data for the item you’re holding.</red>"));
                        return true;
                    }
                    if (!match.isComplete()) {
                        player.sendMessage(mini.deserialize("<yellow>This quest isn’t complete yet.</yellow>"));
                        return true;
                    }

                    // run that quest's template reward_commands
                    runRewards(player, match);

                    // remove quest item from hand
                    if (hand.getAmount() <= 1) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        hand.setAmount(hand.getAmount() - 1);
                        player.getInventory().setItemInMainHand(hand);
                    }

                    // remove quest from active list
                    quests.removeIf(q -> q.getId().equals(heldQuestId));

                    player.sendMessage(mini.deserialize("<green>Claimed your quest reward.</green>"));
                    return true;
                }

                // 3) Fallback: bulk-claim any complete quests in memory (no time gating)
                List<Quest> all = QuestManager.getQuests(player);
                List<Quest> claimable = new java.util.ArrayList<>();
                for (Quest q : all) if (q.isComplete()) claimable.add(q);

                if (claimable.isEmpty()) {
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.no-claimable")));
                    return true;
                }

                for (Quest q : claimable) {
                    runRewards(player, q);
                }
                // remove claimed single-quest papers from inventory
                var contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    var it = contents[i];
                    if (it == null || !it.hasItemMeta()) continue;
                    var tagged = LoreUtils.getQuestId(it);
                    if (tagged == null) continue;
                    boolean wasClaimed = false;
                    for (Quest q : claimable) {
                        if (q.getId().equals(tagged)) { wasClaimed = true; break; }
                    }
                    if (wasClaimed) contents[i] = null;
                }
                player.getInventory().setContents(contents);

                // remove them from active list
                all.removeIf(claimable::contains);

                player.sendMessage(mini.deserialize(MessageConfig.get("quests.claimed")
                        .replace("%amount%", String.valueOf(claimable.size()))));
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("quests.reload")) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.no-permission")));
                    return true;
                }

                File configFile = new File(CCUtilities.getInstance().getDataFolder(), "quests.yml");
                if (!configFile.exists()) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.file-missing")));
                    return true;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                QuestConfig.load(config);
                sender.sendMessage(mini.deserialize(MessageConfig.get("quests.reloaded")));
                return true;
            }

            default -> {
                sender.sendMessage(mini.deserialize(MessageConfig.get("quests.usage")));
                return true;
            }
        }
    }

    private String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private String formatQuestLine(Quest q) {
        // e.g., "MINE_BLOCK  [12/500]" or "KILL_MOBS(ZOMBIE)  [50/100]"
        String type = q.getType().name();
        String tItem = q.getTargetItem();
        String head = tItem == null ? type : (type + "(" + tItem + ")");
        return "<gray>•</gray> <aqua>" + head + "</aqua> <gray>[</gray><yellow>" + q.getProgress() + "</yellow>/<yellow>" + q.getTarget() + "</yellow><gray>]</gray>";
    }

    private void runRewards(Player player, Quest quest) {
        var template = QuestConfig.getTemplate(quest.getQuestId());
        if (template == null) return;
        for (String raw : template.rewardCommands()) {
            String cmd = raw
                    .replace("%player%", player.getName())
                    .replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private Quest timeWarpToClaimable(Quest q, long nowMs) {
        // Make the quest complete and ensure now is in its claim window.
        long expSeconds = q.getExpirationSeconds();       // duration in seconds
        long claimSeconds = q.getClaimWindowSeconds();    // claim window in seconds

        // We want: endTime = startTime + exp*1000  to be slightly before now,
        // and now <= endTime + claimWindow*1000.
        // So pick startTime so that endTime = now - 1000 ms.
        long desiredEndMs = Math.max(0, nowMs - 1000);
        long startTimeWarped = Math.max(0, desiredEndMs - (expSeconds * 1000L));

        return new Quest(
                q.getId(),
                q.getQuestId(),
                q.getType(),
                q.getTarget(),
                q.getTarget(),               // progress = target (complete)
                startTimeWarped,
                q.getExpirationSeconds(),
                claimSeconds,
                q.getTargetItem()
        );
    }
}
