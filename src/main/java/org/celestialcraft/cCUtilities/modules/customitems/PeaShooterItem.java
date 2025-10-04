package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PeaShooterItem implements CustomItem {

    private static final String LORE_LINE = "§7Pea Shooter";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "pea_shooter";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setItem(new ItemStack(Material.SLIME_BALL)); // gives it a green-ish look in-flight

        snowball.getWorld().spawnParticle(
                Particle.DUST,
                snowball.getLocation(),
                5, 0.1, 0.1, 0.1,
                new Particle.DustOptions(Color.LIME, 1.5f)
        );

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!snowball.isValid() || snowball.isDead()) {
                    this.cancel();
                    return;
                }
                snowball.getWorld().spawnParticle(
                        Particle.DUST,
                        snowball.getLocation(),
                        1, 0, 0, 0,
                        new Particle.DustOptions(Color.LIME, 1.5f)
                );
            }
        }.runTaskTimer(org.celestialcraft.cCUtilities.CCUtilities.getInstance(), 1L, 1L);
    }
}
