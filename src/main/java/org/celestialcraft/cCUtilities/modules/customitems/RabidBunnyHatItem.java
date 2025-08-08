package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.util.List;

public class RabidBunnyHatItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Rabid Bunny Hat";
    private static final String UNUSED_LINE = "§7Unused";
    private static final String USED_LINE = "§8Used";
    private static final NamespacedKey OWNER_KEY = new NamespacedKey(CCUtilities.getInstance(), "guard_bunny_owner");
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    @Override
    public String getIdentifier() {
        return "rabid_bunny_hat";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.CARVED_PUMPKIN || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return false;
        List<Component> lore = meta.lore();
        return lore != null && lore.stream().anyMatch(line -> serializer.serialize(line).equals(LORE_IDENTIFIER));
    }

    @Override
    public void onRightClickSneak(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!matches(item)) return;
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.size() < 2) return;

        String secondLine = serializer.serialize(lore.get(1));
        if (!secondLine.equals(UNUSED_LINE)) return;

        lore.set(1, serializer.deserialize(USED_LINE));
        meta.lore(lore);
        item.setItemMeta(meta);

        Location loc = player.getLocation();
        Rabbit bunny = loc.getWorld().spawn(loc, Rabbit.class);
        bunny.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        bunny.customName(Component.text(player.getName() + "'s Guard Bunny"));
        bunny.setCustomNameVisible(true);
        bunny.setRemoveWhenFarAway(false);

        bunny.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
    }

    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Rabbit bunny)) return;
        if (!(event.getTarget() instanceof Player target)) return;

        PersistentDataContainer data = bunny.getPersistentDataContainer();
        if (!data.has(OWNER_KEY, PersistentDataType.STRING)) return;

        String ownerId = data.get(OWNER_KEY, PersistentDataType.STRING);
        if (ownerId != null && target.getUniqueId().toString().equals(ownerId)) {
            event.setCancelled(true);
        }
    }
}
