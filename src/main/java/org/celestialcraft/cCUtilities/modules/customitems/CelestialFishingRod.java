package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CelestialFishingRod implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Random random = new Random();
    private static final NamespacedKey fishKey = NamespacedKey.minecraft("celestial_fish");
    private static final NamespacedKey effectKey = NamespacedKey.minecraft("celestial_effect");
    private static final NamespacedKey levelKey = NamespacedKey.minecraft("celestial_effect_level");

    @Override
    public String getIdentifier() {
        return "celestial_fishing_rod";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) return false;
        List<Component> lore = item.getItemMeta().lore();
        return lore != null && !lore.isEmpty() && serializer.serialize(lore.getFirst()).equals("§7Celestial Fishing Rod");
    }

    @Override
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (!matches(rod)) return;

        if (!(event.getCaught() instanceof org.bukkit.entity.Item itemEntity)) return;

        ItemStack caught = itemEntity.getItemStack();
        Material type = caught.getType();
        String name;
        PotionEffectType effect;

        switch (type) {
            case PUFFERFISH -> {
                name = "Pufferfish";
                effect = PotionEffectType.SPEED;
            }
            case COD -> {
                name = "Cod";
                effect = PotionEffectType.STRENGTH;
            }
            case TROPICAL_FISH -> {
                name = "Tropical Fish";
                effect = PotionEffectType.ABSORPTION;
            }
            case SALMON -> {
                name = "Salmon";
                effect = PotionEffectType.REGENERATION;
            }
            default -> {
                return;
            }
        }

        int level = rollEffectLevel();
        double weight = rollFishWeight();
        String weightFormatted = String.format(Locale.US, "%.2f", weight);

        ItemStack customFish = new ItemStack(type);
        ItemMeta meta = customFish.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Celestial " + name, TextColor.fromHexString("#c1adfe"))
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(formatPotionName(effect) + " " + level)
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Weight: ")
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(weightFormatted + " lbs")
                            .color(TextColor.fromHexString("#c1adfe"))
                            .decoration(TextDecoration.ITALIC, false)));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(fishKey, PersistentDataType.INTEGER, 1);
            meta.getPersistentDataContainer().set(effectKey, PersistentDataType.STRING, effect.getKey().getKey());
            meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
            customFish.setItemMeta(meta);
        }

        itemEntity.setItemStack(customFish);
    }

    @Override
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(fishKey, PersistentDataType.INTEGER)) return;
        String effectName = meta.getPersistentDataContainer().get(effectKey, PersistentDataType.STRING);
        Integer level = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);

        if (effectName == null || level == null) return;

        PotionEffectType effect = org.bukkit.Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effectName));
        if (effect == null) return;

        int duration = switch (level) {
            case 1 -> 20 * 60 * 2;
            case 2 -> 20 * 60;
            default -> 20 * 30;
        };

        event.getPlayer().addPotionEffect(new PotionEffect(effect, duration, level - 1));
    }

    private int rollEffectLevel() {
        int roll = random.nextInt(100);
        if (roll < 10) return 3;
        if (roll < 40) return 2;
        return 1;
    }

    private double rollFishWeight() {
        double base = Math.pow(random.nextDouble(), 3);
        return 100.0 * base;
    }

    private String formatPotionName(PotionEffectType effect) {
        String key = effect.translationKey().replace("effect.minecraft.", "");
        return switch (key) {
            case "speed" -> "Swiftness";
            case "strength" -> "Strength";
            case "absorption" -> "Absorption";
            case "regeneration" -> "Regeneration";
            default -> key;
        };
    }
}
