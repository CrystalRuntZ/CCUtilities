package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.*;

public class MobPacifierItem implements CustomItem {

    private static final String LORE_LINE = "§7Mob Pacifier";
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Set<UUID> pacifiedMobs = new HashSet<>();

    @Override
    public String getIdentifier() {
        return "mob_pacifier";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;

        List<Component> lore = meta.lore();
        if (lore == null) return false;

        return lore.stream().anyMatch(component ->
                serializer.serialize(component).contains(LORE_LINE));
    }

    @SuppressWarnings("deprecation")
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Mob mob)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        UUID mobId = mob.getUniqueId();
        if (pacifiedMobs.contains(mobId)) {
            mob.setAI(true);
            mob.setCustomName(null);
            mob.removeMetadata("no_log", CCUtilities.getInstance());
            pacifiedMobs.remove(mobId);
            player.sendMessage("§cMob reverted to normal.");
        } else {
            mob.setAI(false);
            mob.setCustomName("§7(Pacified) " + mob.getType().name());
            mob.setCustomNameVisible(false);
            mob.setPersistent(true);
            mob.setMetadata("no_log", new FixedMetadataValue(CCUtilities.getInstance(), true));
            pacifiedMobs.add(mobId);
            player.sendMessage("§x§C§1§A§F§D§EMob pacified.");
        }

        event.setCancelled(true);
    }
}
