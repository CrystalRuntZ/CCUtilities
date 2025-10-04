package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class BerserkerItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Berserker";
    private static final long COOLDOWN_MILLIS = 5 * 60 * 1000; // 5 minutes
    private static final long DURATION_TICKS = 20 * 30; // 30 seconds

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> activeBerserkers = new HashSet<>();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "berserker";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MILLIS) {
            long remaining = (COOLDOWN_MILLIS - (now - cooldowns.get(uuid))) / 1000;
            player.sendMessage("§cYou must wait " + remaining + " seconds before using Berserker again.");
            return;
        }

        if (player.getHealth() <= 6.0) {
            player.sendMessage("§cYou need at least 3 full hearts to activate Berserker.");
            return;
        }

        player.setHealth(player.getHealth() - 6.0);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) DURATION_TICKS, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, (int) DURATION_TICKS, 1));

        activeBerserkers.add(uuid);
        cooldowns.put(uuid, now);

        Bukkit.getScheduler().runTaskLater(
                CCUtilities.getInstance(),
                () -> activeBerserkers.remove(uuid),
                DURATION_TICKS
        );

        player.sendMessage("§aBerserker activated! Speed II, Strength II, and Lifesteal enabled for 30 seconds.");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!activeBerserkers.contains(player.getUniqueId())) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        double healAmount = event.getFinalDamage() * 0.20;
        AttributeInstance maxHealthAttrInstance = player.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealthAttrInstance == null) return;

        double maxHealth = maxHealthAttrInstance.getValue();
        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
        player.setHealth(newHealth);

        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3);
    }
}
