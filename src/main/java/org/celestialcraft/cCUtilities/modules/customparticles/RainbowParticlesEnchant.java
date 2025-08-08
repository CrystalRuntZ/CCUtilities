package org.celestialcraft.cCUtilities.modules.customparticles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;

import java.util.List;

public class RainbowParticlesEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Rainbow Particles";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private JavaPlugin plugin;

    @Override
    public String getIdentifier() {
        return "rainbow_particles";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getLoreLine() {
        return RAW_LORE;
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && switch (item.getType()) {
            case NETHERITE_SWORD, DIAMOND_SWORD, IRON_SWORD, STONE_SWORD, WOODEN_SWORD,
                 NETHERITE_AXE, DIAMOND_AXE, IRON_AXE, STONE_AXE, WOODEN_AXE -> true;
            default -> false;
        };
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || hasEnchant(item)) return item;
        lore.add(LORE_LINE);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {
        // No combat effect
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        ParticleManager.register(event.getPlayer(), ParticleEffectType.RAINBOW_SPIRAL);
    }

    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        ParticleManager.register(event.getPlayer(), ParticleEffectType.RAINBOW_SPIRAL);
    }

    @Override
    public void onPlayerMove(Player player) {
        boolean has = hasEnchant(player.getInventory().getItemInMainHand())
                || hasEnchant(player.getInventory().getItemInOffHand());

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (hasEnchant(armor)) {
                has = true;
                break;
            }
        }

        if (has) {
            ParticleManager.register(player, ParticleEffectType.RAINBOW_SPIRAL);
        } else {
            ParticleManager.unregister(player, ParticleEffectType.RAINBOW_SPIRAL);
        }
    }
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
