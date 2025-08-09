package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PoisonedAppleItem implements CustomItem {

    private static final String LORE_LINE = "§7Poisoned Apple";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);

    @Override
    public String getIdentifier() {
        return "poisoned_apple";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.APPLE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_COMPONENT));
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack eaten = event.getItem();
        if (!matches(eaten)) return;

        int roll = ThreadLocalRandom.current().nextInt(1, 5); // 1..4
        if (roll == 2) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.POISON, 30 * 20, 4, true, true));
        } else {
            event.getPlayer().setFoodLevel(20);
            event.getPlayer().setSaturation(20f);
        }
    }
}
