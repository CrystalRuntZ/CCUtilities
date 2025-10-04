package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomPotionEffectItem implements CustomItem {

    private static final String RAW_LORE = "&7Random Potion Effect";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);
    private static final List<String> ALLOWED_WORLDS = List.of("wild", "wild_nether", "wild_the_end", "spawnworld");

    private final Random random = new Random();
    private final List<PotionEffectType> validEffects = new ArrayList<>();

    public RandomPotionEffectItem() {
        validEffects.addAll(List.of(
                PotionEffectType.POISON,
                PotionEffectType.SLOWNESS,
                PotionEffectType.BLINDNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.NAUSEA,
                PotionEffectType.WITHER,
                PotionEffectType.MINING_FATIGUE
        ));
    }

    @Override
    public String getIdentifier() {
        return "random_potion_effect_item";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.getType().isItem() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        String worldName = player.getWorld().getName().toLowerCase();
        if (!ALLOWED_WORLDS.contains(worldName)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (random.nextDouble() <= 0.20) {
            PotionEffectType effectType = validEffects.get(random.nextInt(validEffects.size()));
            int amplifier = random.nextInt(4);
            int duration = (5 + random.nextInt(26)) * 20;
            target.addPotionEffect(new PotionEffect(effectType, duration, amplifier));

            player.sendActionBar(Component.text("§aApplied " + effectType.getName().toLowerCase().replace('_', ' ') +
                    " to " + target.getName() + " for " + (duration/20) + " seconds!"));
        }
    }
}
