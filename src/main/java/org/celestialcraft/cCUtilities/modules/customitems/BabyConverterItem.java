package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BabyConverterItem implements CustomItem {

    private static final String LORE_LINE = "§7Baby Converter";
    private static final Component LORE_COMPONENT = LegacyComponentSerializer.legacySection().deserialize(LORE_LINE);
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static final Set<EntityType> BLOCKED_MOBS = Set.of(
            EntityType.TURTLE,
            EntityType.WITHER,
            EntityType.ENDER_DRAGON
    );

    @Override
    public String getIdentifier() {
        return "baby_converter";
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
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Entity clicked = event.getRightClicked();

        if (!matches(item)) return;

        if (!(clicked instanceof Ageable ageable)) {
            player.sendMessage(mm.deserialize("<red>You can only transform mobs!"));
            return;
        }

        if (BLOCKED_MOBS.contains(clicked.getType())) {
            player.sendMessage(mm.deserialize("<red>This mob cannot be transformed!"));
            return;
        }

        if (ageable.getAge() < 0) {
            if (player.isSneaking()) {
                ageable.setAge(0);
                player.sendMessage(mm.deserialize("<green>You have turned the baby <white>" + clicked.getName() + " <green>into an adult!"));
            } else {
                player.sendMessage(mm.deserialize("<red>Crouch + Right click to turn a baby mob into an adult!"));
            }
        } else {
            ageable.setAge(-2147483647);
            player.sendMessage(mm.deserialize("<green>You have turned the <white>" + clicked.getName() + " <green>into a baby!"));
        }
    }
}
