package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MobPacifierItem implements CustomItem, Listener {

    private static final String LORE_LINE = "§7Mob Pacifier";
    private static final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private final Set<UUID> pacifiedMobs = new HashSet<>();
    private final JavaPlugin plugin;

    public MobPacifierItem(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPacifiedMobs();
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getIdentifier() {
        return "mob_pacifier";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore())
            return false;
        List<Component> lore = meta.lore();
        if (lore == null)
            return false;

        return lore.stream()
                .anyMatch(component -> serializer.serialize(component).equals(LORE_LINE));
    }

    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Mob mob)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        UUID mobId = mob.getUniqueId();

        if (pacifiedMobs.contains(mobId)) {
            revertMob(mob, player);
        } else {
            pacifyMob(mob, player);
        }

        event.setCancelled(true);
    }

    private void pacifyMob(Mob mob, Player player) {
        mob.setAI(false);
        mob.customName(Component.text("(Pacified) ")
                .color(TextColor.color(0x7F7F7F))
                .append(Component.text(mob.getType().name())));
        mob.setCustomNameVisible(false);
        mob.setPersistent(true);
        mob.setMetadata("no_log", new FixedMetadataValue(plugin, true));
        pacifiedMobs.add(mob.getUniqueId());
        player.sendMessage("§x§C§1§A§F§DMob pacified.");
        savePacifiedMobs();
    }

    private void revertMob(Mob mob, Player player) {
        mob.setAI(true);
        mob.customName(null);
        mob.removeMetadata("no_log", plugin);
        pacifiedMobs.remove(mob.getUniqueId());
        player.sendMessage("§cMob reverted to normal.");
        savePacifiedMobs();
    }

    private void loadPacifiedMobs() {
        var config = plugin.getConfig();
        List<String> uuids = config.getStringList("pacified-mobs");
        for (String uuidStr : uuids) {
            try {
                pacifiedMobs.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void savePacifiedMobs() {
        List<String> uuids = pacifiedMobs.stream().map(UUID::toString).toList();
        plugin.getConfig().set("pacified-mobs", uuids);
        plugin.saveConfig();
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!pacifiedMobs.contains(mob.getUniqueId())) return;

        // Reapply pacification to the mob on spawn
        mob.setAI(false);
        mob.customName(Component.text("(Pacified) ")
                .color(TextColor.color(0x7F7F7F))
                .append(Component.text(mob.getType().name())));
        mob.setCustomNameVisible(false);
        mob.setPersistent(true);
        mob.setMetadata("no_log", new FixedMetadataValue(plugin, true));
    }
}
