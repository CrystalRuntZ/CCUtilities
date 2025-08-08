package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class PhantomBattleAxeItem implements CustomItem {

    private static final String LORE_LINE = "§7Phantom Battle Axe";
    private static final long COOLDOWN_MS = 30_000;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "phantom_battle_axe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        for (Component line : Objects.requireNonNull(meta.lore())) {
            if (serializer.serialize(line).equals(LORE_LINE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (!matches(player.getInventory().getItemInMainHand())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        Location spawnLoc = target.getLocation();

        for (int i = 0; i < 3; i++) {
            Phantom phantom = (Phantom) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.PHANTOM);
            phantom.setTarget(target);
            phantom.customName(Component.text("Phantom Guardian"));
            phantom.setSilent(true);
            phantom.setRemoveWhenFarAway(true);
            phantom.setInvisible(false);

            spawnLoc.getWorld().spawnParticle(Particle.ASH, phantom.getLocation(), 30, 0.3, 0.5, 0.3, 0.01);
            spawnLoc.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.0f);

            new BukkitRunnable() {
                int cycles = 0;
                @Override
                public void run() {
                    if (phantom.isDead() || target.isDead() || cycles++ >= 5) {
                        Location loc = phantom.getLocation();
                        loc.getWorld().spawnParticle(Particle.ASH, loc, 30, 0.3, 0.5, 0.3, 0.01);
                        loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_DEATH, 1.0f, 1.0f);
                        phantom.remove();
                        cancel();
                        return;
                    }
                    phantom.setTarget(target);
                }
            }.runTaskTimer(CCUtilities.getInstance(), 0L, 60L);
        }
    }
}
