package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BatWingsItem implements CustomItem {

    private static final String LORE_SECT = "§7Bat Wings";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Map<UUID, Double> glideBaselineHz = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "bat_wings";
    }

    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        for (var c : lore) {
            if (c == null) continue;
            if (LEGACY.serialize(c).equals(LORE_SECT)) return true;
        }
        return false;
    }

    private static boolean hasEffectEquippedOrHeld(Player p) {
        PlayerInventory inv = p.getInventory();
        if (hasLoreLine(inv.getItemInMainHand()) || hasLoreLine(inv.getItemInOffHand())) return true;
        for (ItemStack armor : inv.getArmorContents())
            if (hasLoreLine(armor)) return true;
        return false;
    }

    private static boolean hasLoreLine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        var lore = meta.lore();
        if (lore == null || lore.isEmpty()) return false;
        for (var c : lore) {
            if (c == null) continue;
            if (LEGACY.serialize(c).equals(LORE_SECT)) return true;
        }
        return false;
    }

    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.isGliding()) {
            if (hasEffectEquippedOrHeld(p)) {
                Vector v = p.getVelocity();
                double hz = Math.hypot(v.getX(), v.getZ());
                glideBaselineHz.put(p.getUniqueId(), Math.max(hz, 0.6));
            } else {
                glideBaselineHz.remove(p.getUniqueId());
            }
        } else {
            glideBaselineHz.remove(p.getUniqueId());
        }
    }

    @Override
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!p.isGliding()) return;
        if (!hasEffectEquippedOrHeld(p)) return;

        glideBaselineHz.computeIfAbsent(p.getUniqueId(), id -> {
            Vector v = p.getVelocity();
            double hz = Math.hypot(v.getX(), v.getZ());
            return Math.max(hz, 0.6);
        });

        double base = glideBaselineHz.getOrDefault(p.getUniqueId(), 0.6);
        double target = base * 1.10;

        Vector v = p.getVelocity();
        double currHz = Math.hypot(v.getX(), v.getZ());
        if (currHz >= target - 1e-4) return;

        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1e-6) return;
        dir.normalize();

        double missing = target - currHz;
        double boost = Math.min(missing, 0.08);
        Vector add = dir.multiply(boost);

        p.setVelocity(v.add(new Vector(add.getX(), 0.0, add.getZ())));
    }
}
