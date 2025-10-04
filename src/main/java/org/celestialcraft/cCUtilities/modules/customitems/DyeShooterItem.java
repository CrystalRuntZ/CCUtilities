    package org.celestialcraft.cCUtilities.modules.customitems;

    import net.kyori.adventure.text.Component;
    import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
    import org.bukkit.*;
    import org.bukkit.entity.Item;
    import org.bukkit.entity.Player;
    import org.bukkit.event.player.PlayerInteractEvent;
    import org.bukkit.inventory.EquipmentSlot;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.meta.ItemMeta;

    import java.util.*;

    public class DyeShooterItem implements CustomItem {

        private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize("§7Dye Shooter");
        private final Map<UUID, Long> cooldowns = new HashMap<>();
        private final List<Material> dyeTypes = List.of(
                Material.BLACK_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.CYAN_DYE,
                Material.GRAY_DYE, Material.GREEN_DYE, Material.LIGHT_BLUE_DYE, Material.LIGHT_GRAY_DYE,
                Material.LIME_DYE, Material.MAGENTA_DYE, Material.ORANGE_DYE, Material.PINK_DYE,
                Material.PURPLE_DYE, Material.RED_DYE, Material.WHITE_DYE, Material.YELLOW_DYE
        );
        private final Random random = new Random();

        @Override
        public String getIdentifier() {
            return "dye_shooter";
        }

        @Override
        public boolean matches(ItemStack item) {
            if (item == null || !item.hasItemMeta()) return false;
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasLore()) return false;
            List<Component> lore = meta.lore();
            if (lore == null) return false;
            // Check if any lore line equals LORE_LINE
            for (Component line : lore) {
                if (line.equals(LORE_LINE)) return true;
            }
            return false;
        }

        @Override
        public void onRightClick(PlayerInteractEvent event) {
            if (event.getHand() != EquipmentSlot.HAND) return;

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < 3000) {
                event.setCancelled(true);
                return;
            }
            cooldowns.put(uuid, now);

            Location eyeLoc = player.getEyeLocation();
            Material dyeType = dyeTypes.get(random.nextInt(dyeTypes.size()));
            ItemStack dyeItem = new ItemStack(dyeType);

            Item thrown = player.getWorld().dropItem(eyeLoc, dyeItem);
            thrown.setVelocity(eyeLoc.getDirection().normalize().multiply(1.5));

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

            Color dustColor = switch (dyeType) {
                case BLACK_DYE -> Color.fromRGB(25, 25, 25);
                case BLUE_DYE -> Color.fromRGB(50, 70, 230);
                case BROWN_DYE -> Color.fromRGB(100, 60, 20);
                case CYAN_DYE -> Color.fromRGB(30, 170, 170);
                case GRAY_DYE -> Color.fromRGB(100, 100, 100);
                case GREEN_DYE -> Color.fromRGB(80, 160, 40);
                case LIGHT_BLUE_DYE -> Color.fromRGB(130, 190, 230);
                case LIGHT_GRAY_DYE -> Color.fromRGB(180, 180, 180);
                case LIME_DYE -> Color.fromRGB(140, 250, 70);
                case MAGENTA_DYE -> Color.fromRGB(180, 60, 180);
                case ORANGE_DYE -> Color.fromRGB(250, 130, 0);
                case PINK_DYE -> Color.fromRGB(250, 140, 140);
                case PURPLE_DYE -> Color.fromRGB(100, 70, 170);
                case RED_DYE -> Color.fromRGB(220, 0, 0);
                case YELLOW_DYE -> Color.fromRGB(250, 250, 70);
                default -> Color.fromRGB(255, 255, 255);
            };

            player.getWorld().spawnParticle(Particle.DUST, eyeLoc, 15, 0.1, 0.1, 0.1,
                    new Particle.DustOptions(dustColor, 1));
        }

    }
