package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.util.List;
import java.util.Random;

public class HalloweenKillAnnouncerEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Halloween Kill Announcer";
    private static final Random RANDOM = new Random();

    private static final List<String> MESSAGES = List.of(
            "<#c1adfe>🎃 <player></#c1adfe> <gray>has stabbed</gray> <#c1adfe><victim></#c1adfe> <gray>to death.</gray>",
            "<#c1adfe>🎃 <victim></#c1adfe> <gray>was betrayed by their friend,</gray> <#c1adfe><player></#c1adfe><gray>.</gray>",
            "<#c1adfe>🎃</#c1adfe> <gray>Ki, ki, ki... Ma, ma, ma...</gray> <#c1adfe><victim></#c1adfe> <gray>traveled to Camp Crystal Lake with</gray> <#c1adfe><player></#c1adfe><gray>.</gray>",
            "<#c1adfe>🎃 <victim></#c1adfe> <gray>has succumbed to</gray> <#c1adfe><player></#c1adfe> <gray>within the dream world.</gray>",
            "<#c1adfe>🎃 <victim></#c1adfe> <gray>failed</gray> <#c1adfe><player></#c1adfe><gray>'s game.</gray>"
    );

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // To prevent multiple messages per death, track announced players (victims) temporarily
    private final List<Player> recentlyAnnounced = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public String getIdentifier() {
        return "halloween_kill_announcer";
    }

    @Override
    public String getLoreLine() {
        return RAW_LORE;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !appliesTo(item) || hasEnchant(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player killer)) return;

        // Avoid multiple announcements per victim by checking recently announced list
        if (recentlyAnnounced.contains(victim)) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!hasEnchant(weapon)) return;

        if (victim.getHealth() - event.getFinalDamage() > 0) return; // Not fatal damage

        // Mark victim as announced
        recentlyAnnounced.add(victim);

        // Schedule removal from announced list after a short delay (e.g. 10 seconds)
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> recentlyAnnounced.remove(victim), 200L);

        // Pick random message and replace placeholders
        String rawMessage = MESSAGES.get(RANDOM.nextInt(MESSAGES.size()));
        rawMessage = rawMessage.replace("<player>", killer.getName())
                .replace("<victim>", victim.getName());

        // Parse mini message colors and send to all players
        Component msg = MINI_MESSAGE.deserialize(rawMessage);
        CCUtilities.getInstance().getServer().getOnlinePlayers().forEach(p -> p.sendMessage(msg));
    }
}
