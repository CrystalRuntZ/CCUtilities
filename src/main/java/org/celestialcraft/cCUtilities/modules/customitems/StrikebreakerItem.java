package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class StrikebreakerItem implements CustomItem {

    private static final String RAW_LORE = "&7Strikebreaker";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    private static final int FIRE_RATE_TICKS = 3; // ~8 arrows/sec
    private static final int MAX_ARROWS = 32;
    private static final long OVERHEAT_DURATION = 40L; // 2 seconds

    private final Set<UUID> overheating = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "strikebreaker";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (overheating.contains(uuid) || activeTasks.containsKey(uuid)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        BukkitRunnable task = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.isValid()) {
                    stopFiring();
                    return;
                }

                if (count >= MAX_ARROWS) {
                    overheat(player);
                    stopFiring();
                    return;
                }

                ItemStack arrowItem = extractArrow(player);
                if (arrowItem == null) {
                    player.sendActionBar(Component.text("Out of arrows!", NamedTextColor.RED));
                    stopFiring();
                    return;
                }

                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                arrow.setDamage(0.1);
                arrow.setVelocity(player.getLocation().getDirection().normalize().multiply(2));
                arrow.setCritical(false);

                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5f, 1.2f);
                player.sendActionBar(Component.text("Strikebreaker: ", NamedTextColor.RED)
                        .append(Component.text((MAX_ARROWS - count) + " arrows remaining", NamedTextColor.WHITE)));

                count++;
            }

            private void stopFiring() {
                cancel();
                activeTasks.remove(uuid);
            }
        };

        activeTasks.put(uuid, task);
        task.runTaskTimer(CCUtilities.getInstance(), 0L, FIRE_RATE_TICKS);
    }

    private void overheat(Player player) {
        UUID uuid = player.getUniqueId();
        overheating.add(uuid);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 50, 0.4, 0.2, 0.4, 0.02);

        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> overheating.remove(uuid), OVERHEAT_DURATION);
    }

    private ItemStack extractArrow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            Material type = item.getType();
            if (type == Material.ARROW || type == Material.SPECTRAL_ARROW || type == Material.TIPPED_ARROW) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    player.getInventory().removeItem(item);
                }
                return item;
            }
        }
        return null;
    }
}
