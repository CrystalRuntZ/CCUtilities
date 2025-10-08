package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JumpScareWand implements CustomItem {

    private static final String RAW_LORE = "§7Jumpscare Wand";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final long COOLDOWN_MS = 60_000L;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<UUID, Long> lastScared = new HashMap<>();

    @Override
    public String getIdentifier() {
        return "jumpscare_wand";
    }

    @Override
    public boolean matches(ItemStack item) {
        return hasLoreLine(item);
    }

    private boolean hasLoreLine(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta.lore() == null) {
            return false;
        }
        String target = RAW_LORE.replace('&', '§');
        for (var c : Objects.requireNonNull(meta.lore())) {
            if (c == null) continue;
            String s = LEGACY.serialize(c);
            if (s.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRightClick(Action a) {
        return a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean offCooldown(Map<UUID, Long> map, UUID id) {
        long now = System.currentTimeMillis();
        Long last = map.get(id);
        return last == null || (now - last) >= COOLDOWN_MS;
    }

    @Override
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!isRightClick(e.getAction())) {
            return;
        }

        Player p = e.getPlayer();
        if (!p.isSneaking()) {
            return;
        }

        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (!hasLoreLine(inHand)) {
            return;
        }

        if (!offCooldown(lastUse, p.getUniqueId())) {
            long rem = COOLDOWN_MS - (System.currentTimeMillis() - lastUse.get(p.getUniqueId()));
            long secs = Math.max(1, rem / 1000);
            p.sendActionBar(Component.text("⏳ Jumpscare cooldown: " + secs + "s"));
            return;
        }

        List<Player> candidates = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            if (offCooldown(lastScared, other.getUniqueId())) {
                candidates.add(other);
            }
        }

        if (candidates.isEmpty()) {
            p.sendActionBar(Component.text("No valid targets right now."));
            return;
        }

        Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        long now = System.currentTimeMillis();
        lastUse.put(p.getUniqueId(), now);
        lastScared.put(target.getUniqueId(), now);

        target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, true, false, false));

        p.sendActionBar(
                Component.text("🎃 ").color(TextColor.fromHexString("#c1adfe"))
                        .append(Component.text("You have scared ").color(TextColor.fromHexString("#ffffff")))
                        .append(Component.text(target.getName()).color(TextColor.fromHexString("#c1adfe")))
                        .append(Component.text("!").color(TextColor.fromHexString("#ffffff")))
        );
    }
}
