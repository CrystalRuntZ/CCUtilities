package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BedrockPickaxeItem implements CustomItem {

    private static final String LORE_IDENTIFIER = "§7Bedrock Pickaxe";
    private static final String USES_PREFIX = "§7Uses Left: ";
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether");

    @Override
    public String getIdentifier() {
        return "bedrock_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return false;
        for (Component line : lore) {
            if (legacy.serialize(line).equals(LORE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block target = event.getClickedBlock();
        if (target == null || target.getType() != Material.BEDROCK) return;

        if (!ALLOWED_WORLDS.contains(target.getWorld().getName())) return;

        Player player = event.getPlayer();
        ItemStack tool = event.getHand() == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        if (!matches(tool)) return;

        if (target.getY() <= -64) {
            player.sendMessage("§cYou cannot break the bottom bedrock layer!");
            return;
        }

        target.setType(Material.AIR);
        target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.BEDROCK));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
        target.getWorld().spawnParticle(
                Particle.BLOCK_CRUMBLE,
                target.getLocation().add(0.5, 0.5, 0.5),
                30, 0.25, 0.25, 0.25,
                Material.BEDROCK.createBlockData()
        );

        ItemMeta meta = tool.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null) return;

        List<Component> newLore = new ArrayList<>(lore);
        for (int i = 0; i < newLore.size(); i++) {
            String plainLine = legacy.serialize(newLore.get(i));
            if (plainLine.startsWith(USES_PREFIX)) {
                try {
                    int uses = Integer.parseInt(plainLine.substring(USES_PREFIX.length()));
                    uses--;
                    if (uses > 0) {
                        newLore.set(i, legacy.deserialize(USES_PREFIX + uses));
                        meta.lore(newLore);
                        tool.setItemMeta(meta);
                    } else {
                        player.sendMessage("§cYour Bedrock Pickaxe has broken!");
                        if (event.getHand() == EquipmentSlot.HAND) {
                            player.getInventory().setItemInMainHand(null);
                        } else {
                            player.getInventory().setItemInOffHand(null);
                        }
                    }
                    event.setCancelled(true);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid uses count on Bedrock Pickaxe.");
                }
                break;
            }
        }
    }
}
