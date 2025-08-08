package org.celestialcraft.cCUtilities.modules.customparticles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;

import java.util.List;

public class FlameRingEnchant implements CustomEnchant {

    private static final String LORE_LINE = "§7Flame Ring Particles";
    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "flame_ring_particles";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        return hasEnchant(item);
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                legacy.serialize(component).equalsIgnoreCase(LORE_LINE));
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return null;
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getLoreLine() {
        return "&7Flame Ring Particles";
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        ParticleManager.register(event.getPlayer(), ParticleEffectType.FLAME_RING);
    }

    @Override
    public void onHandSwap(Player player) {
        ParticleManager.register(player, ParticleEffectType.FLAME_RING);
    }

    @Override
    public void onPlayerMove(Player player) {
        boolean hasEffect = hasEnchant(player.getInventory().getItemInMainHand())
                || hasEnchant(player.getInventory().getItemInOffHand());

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) {
                hasEffect = true;
                break;
            }
        }

        if (hasEffect) {
            ParticleManager.register(player, ParticleEffectType.FLAME_RING);
        } else {
            ParticleManager.unregister(player, ParticleEffectType.FLAME_RING);
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
