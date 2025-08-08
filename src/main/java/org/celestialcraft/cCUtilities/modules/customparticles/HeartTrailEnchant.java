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

public class HeartTrailEnchant implements CustomEnchant {

    private static final String LORE_IDENTIFIER = "&7Heart Trail Particles";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private JavaPlugin plugin;


    @Override
    public String getIdentifier() {
        return "heart_trail_particles";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        String formatted = LORE_IDENTIFIER.replace("&", "§");
        for (Component line : lore) {
            if (legacy.serialize(line).equalsIgnoreCase(formatted)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return false;
    }

    @Override
    public void applyEffect(EntityDamageByEntityEvent event) {

    }

    @Override
    public ItemStack applyTo(ItemStack item) {
        return null;
    }

    @Override
    public String getLoreLine() {
        return "&7Heart Trail Particles";
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ParticleManager.register(player, ParticleEffectType.HEART_TRAIL);
    }

    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ParticleManager.register(player, ParticleEffectType.HEART_TRAIL);
    }

    @Override
    public void onPlayerMove(Player player) {
        boolean hasItem = appliesTo(player.getInventory().getItemInMainHand()) ||
                appliesTo(player.getInventory().getItemInOffHand());

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (appliesTo(armor)) {
                hasItem = true;
                break;
            }
        }

        if (hasItem) {
            ParticleManager.register(player, ParticleEffectType.HEART_TRAIL);
        } else {
            ParticleManager.unregister(player, ParticleEffectType.HEART_TRAIL);
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
