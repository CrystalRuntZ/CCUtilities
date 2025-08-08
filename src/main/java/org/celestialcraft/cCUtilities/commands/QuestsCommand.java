package org.celestialcraft.cCUtilities.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.MessageConfig;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;
import org.celestialcraft.cCUtilities.modules.quests.QuestManager;
import org.celestialcraft.cCUtilities.modules.quests.config.QuestConfig;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.storage.QuestCooldowns;
import org.celestialcraft.cCUtilities.modules.quests.util.QuestItemFactory;
import org.celestialcraft.cCUtilities.modules.quests.util.WeeklyQuestGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class QuestsCommand implements CommandExecutor {
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mini.deserialize(MessageConfig.get("quests.player-only")));
                    return true;
                }

                long now = System.currentTimeMillis();
                Long lastClaim = QuestCooldowns.getLastWeeklyClaim(player.getUniqueId());
                long oneWeek = 7L * 24 * 60 * 60 * 1000;

                if (lastClaim != null && now - lastClaim < oneWeek) {
                    long remaining = oneWeek - (now - lastClaim);
                    long hours = remaining / 1000 / 60 / 60;
                    long minutes = (remaining / 1000 / 60) % 60;
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.weekly-cooldown")
                            .replace("%hours%", String.valueOf(hours))
                            .replace("%minutes%", String.valueOf(minutes))));
                    return true;
                }

                List<Quest> generated = WeeklyQuestGenerator.generateWeeklyQuests(player.getUniqueId());

                for (Quest quest : generated) {
                    player.getInventory().addItem(QuestItemFactory.createQuestItem(quest));
                    QuestManager.addQuest(player, quest);
                }

                QuestManager.saveWeeklyQuests(player.getUniqueId(), generated);
                QuestCooldowns.setLastWeeklyClaim(player.getUniqueId(), now);

                player.sendMessage(mini.deserialize(MessageConfig.get("quests.weekly-claimed")));
                return true;
            }

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

                List<Quest> claimed = QuestManager.claimCompletedQuests(player);
                if (claimed.isEmpty()) {
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.no-claimable")));
                } else {
                    player.sendMessage(mini.deserialize(MessageConfig.get("quests.claimed").replace("%amount%", String.valueOf(claimed.size()))));
                }
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
}
