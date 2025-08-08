package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

public class CyberCottonCandyItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "&7Cyber Cotton Candy";
    private static final Random random = new Random();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "cyber_cotton_candy";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
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
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        player.setFoodLevel(20);
        player.setSaturation(20f);

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(getClass());
        plugin.getSLF4JLogger().info("[CyberCottonCandyItem] Player consumed: {}", item.getType());

        if (random.nextInt(100) < 20) {
            List<PotionEffect> effects = List.of(
                    PotionEffectType.SPEED.createEffect(20 * 15, 0),
                    PotionEffectType.WITHER.createEffect(20 * 5, 0),
                    PotionEffectType.JUMP_BOOST.createEffect(20 * 15, 0),
                    PotionEffectType.STRENGTH.createEffect(20 * 15, 0)
            );

            PotionEffect chosen = effects.get(random.nextInt(effects.size()));
            plugin.getSLF4JLogger().info("[CyberCottonCandyItem] Applying effect: {}", chosen.getType().translationKey());

            boolean success = player.addPotionEffect(chosen);
            if (!success) {
                plugin.getSLF4JLogger().warn("[CyberCottonCandyItem] Failed to apply effect: {}", chosen.getType().translationKey());
            }
        }
    }
}
