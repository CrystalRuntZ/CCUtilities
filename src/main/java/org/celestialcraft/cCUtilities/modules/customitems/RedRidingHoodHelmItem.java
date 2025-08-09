package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class RedRidingHoodHelmItem implements CustomItem {

    private static final String LORE_LINE = "§7Red Riding Hood";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    public RedRidingHoodHelmItem() {
        JavaPlugin plugin = CCUtilities.getInstance();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack helm = p.getInventory().getHelmet();
                boolean wearing = matches(helm);

                if (wearing) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false));

                    final double minKeepTargetDist2 = 25.0; // 5 blocks squared

                    for (Entity e : p.getNearbyEntities(30, 30, 30)) {
                        if (!(e instanceof LivingEntity)) continue;

                        if (e instanceof Wolf wolf && !wolf.isTamed()) {
                            wolf.setTarget(p);
                            continue;
                        }

                        if (e instanceof Zombie || e instanceof Skeleton || e instanceof Creeper || e instanceof Spider) {
                            Mob mob = (Mob) e;
                            if (e.getLocation().distanceSquared(p.getLocation()) > minKeepTargetDist2) {
                                if (p.equals(mob.getTarget())) {
                                    mob.setTarget(null);
                                }
                            }
                        }
                    }
                } else {
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
            }
        }, 0L, 40L);
    }

    @Override
    public String getIdentifier() {
        return "red_riding_hood_helm";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }
}
