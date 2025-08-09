package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class InvisibilityEffectEnchant implements CustomEnchant {

    private static final String LORE_LINE = "§7Invisibility Effect";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    public InvisibilityEffectEnchant() {
        JavaPlugin plugin = CCUtilities.getInstance();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (hasEnchantAnywhere(p)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false));
                }
            }
        }, 0L, 40L);
    }

    @Override
    public String getIdentifier() {
        return "invisibility_effect";
    }

    @Override
    public boolean appliesTo(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
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
        return "";
    }

    private boolean hasEnchantAnywhere(Player p) {
        if (appliesTo(p.getInventory().getItemInMainHand())) return true;
        if (appliesTo(p.getInventory().getItemInOffHand())) return true;
        ItemStack[] armor = p.getInventory().getArmorContents();
        for (ItemStack piece : armor) if (appliesTo(piece)) return true;
        return false;
    }
}
