package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class SurrenderItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<UUID, Long> recentCombat = new HashMap<>();
    private final Map<UUID, Long> cooldownMap = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "surrender";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) return false;
        for (Component line : Objects.requireNonNull(item.getItemMeta().lore())) {
            if ("§7Surrender".equals(serializer.serialize(line))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();

        ItemStack item = event.getItem();
        if (item == null || !matches(item)) return;

        if (!isInCombat(player)) {
            player.sendMessage("§cYou can only use this while in combat.");
            return;
        }

        long now = System.currentTimeMillis();
        long lastUse = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUse < 600_000) {
            long remaining = (600_000 - (now - lastUse) + 999) / 1000; // round up seconds
            player.sendActionBar(
                    Component.text("§cSurrender cooldown: " + remaining + "s remaining.")
            );
            event.setCancelled(true);
            return;
        }

        cooldownMap.put(player.getUniqueId(), now);

        // Drop skull
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(mini.deserialize("<#c1adfe>Skull of " + player.getName()));
            meta.lore(List.of(Component.text("Obtained by surrendering.", NamedTextColor.GRAY)));
            skull.setItemMeta(meta);
        }

        player.getWorld().dropItemNaturally(player.getLocation(), skull);
        player.sendMessage("§7You have surrendered. Your skull has been left behind.");

        // Broadcast surrender message
        String opponentName = findLastOpponent(player);
        String message = "§7☆ §x§c§1§a§d§f§e" + player.getName()
                + " §7has surrendered"
                + (opponentName != null ? " to §x§c§1§a§d§f§e" + opponentName : "")
                + " §7and left their skull behind.";
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));

        // Teleport to spawn
        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "espawn " + player.getName()), 1L);

        event.setCancelled(true);
    }

    public void onCombat(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();

        if (event.getDamager() instanceof Player damager) {
            recentCombat.put(damager.getUniqueId(), now);
        }

        if (event.getEntity() instanceof Player victim) {
            recentCombat.put(victim.getUniqueId(), now);
        }
    }

    private boolean isInCombat(Player player) {
        Long last = recentCombat.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last <= 15_000;
    }

    private String findLastOpponent(Player player) {
        UUID self = player.getUniqueId();
        long now = System.currentTimeMillis();
        long recentTime = 0;
        String opponentName = null;

        for (Map.Entry<UUID, Long> entry : recentCombat.entrySet()) {
            if (entry.getKey().equals(self)) continue;
            long time = entry.getValue();
            if (now - time <= 15_000 && time > recentTime) {
                Player opponent = Bukkit.getPlayer(entry.getKey());
                if (opponent != null && opponent.isOnline()) {
                    recentTime = time;
                    opponentName = opponent.getName();
                }
            }
        }

        return opponentName;
    }
}
