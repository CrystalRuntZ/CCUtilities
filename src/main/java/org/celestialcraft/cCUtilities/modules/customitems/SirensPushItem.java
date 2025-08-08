package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SirensPushItem implements CustomItem {

    private static final String RAW_LORE = "&7Siren's Push";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private static final long COOLDOWN_MILLIS = 60 * 1000;
    private static final int INVIS_DURATION_TICKS = 100; // 5 seconds

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "sirens_push";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    public void onInteract(Player player, ItemStack item, PlayerInteractEvent event) {
        if (!matches(item)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < COOLDOWN_MILLIS) return;
        if (!hasSirenPushEquipped(player)) return;

        cooldowns.put(uuid, now);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, INVIS_DURATION_TICKS, 1));

        Bukkit.getScheduler().runTaskLater(CCUtilities.getInstance(), () -> {
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (target.equals(player)) continue;

                Vector push = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize().multiply(2.0).setY(0.75);
                target.setVelocity(push);
            }

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 1, 1, 1, 0.05);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 10);
            player.sendMessage("§eSiren's Push activated!");
        }, INVIS_DURATION_TICKS);
    }

    private boolean hasSirenPushEquipped(Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        return leggings != null && matches(leggings);
    }
}
