package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class UltimateInvisibilityWand implements CustomItem, Listener {

    private static final String RAW_LORE = "§7Ultimate Invisibility Wand";
    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize(RAW_LORE);

    @Override
    public String getIdentifier() {
        return "ultimate_invisibility_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        return lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    @Override
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        handleInteractEntity(event.getPlayer(), event.getRightClicked());
    }

    private void handleInteractEntity(Player player, org.bukkit.entity.Entity clicked) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!matches(inHand)) return;

        if (clicked instanceof ItemFrame frame) {
            boolean newVisible = !frame.isVisible();
            frame.setVisible(newVisible);
            return;
        }

        if (clicked instanceof LivingEntity target) {
            EntityType type = target.getType();

            if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON || type == EntityType.WARDEN) {
                player.sendMessage(Component.text("This wand cannot affect that mob."));
                return;
            }

            boolean nowInvisible = !target.isInvisible();

            target.setInvisible(nowInvisible);

            if (target instanceof Creature c) {
                c.setAI(!nowInvisible);
            }

            if (nowInvisible) {
                player.sendMessage(Component.text("Target is now invisible and its AI has been disabled."));
            } else {
                player.sendMessage(Component.text("Target is now visible and its AI has been re-enabled."));
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (event.getHand() != EquipmentSlot.HAND) return;
        if (inHand.getType() == Material.AIR) return;

        if (!matches(inHand)) return;

        boolean newVisible = !frame.isVisible();
        frame.setVisible(newVisible);

        player.sendMessage(Component.text("Item Frame visibility toggled: " + (newVisible ? "Visible" : "Invisible")));

        event.setCancelled(true);
    }



    @Override
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // not used for this item
    }

}
