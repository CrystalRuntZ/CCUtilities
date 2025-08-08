package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;

import java.util.*;

public class VoidPickaxeItem implements CustomItem {

    private static final Component LORE_LINE = LegacyComponentSerializer.legacySection().deserialize("§7Void Pickaxe");
    private static final Set<String> ALLOWED_WORLDS = Set.of("wild", "wild_nether", "wild_the_end", "mapart");

    private static final Set<Material> BLOCKED_TYPES = EnumSet.of(
            Material.CHEST,
            Material.BARREL,
            Material.SHULKER_BOX,
            Material.BLACK_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.BEDROCK,
            Material.REINFORCED_DEEPSLATE,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN
    );

    @Override
    public String getIdentifier() {
        return "void_pickaxe";
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.getType().toString().endsWith("_PICKAXE")) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<Component> lore = item.getItemMeta().lore();
        return lore != null && lore.stream().anyMatch(line -> line.equals(LORE_LINE));
    }

    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();
        Material type = block.getType();

        if (!matches(tool)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!ALLOWED_WORLDS.contains(block.getWorld().getName())) return;
        if (type == Material.AIR || BLOCKED_TYPES.contains(type)) return;
        if (!ClaimUtils.canBuild(player, block.getLocation())) return;

        boolean silkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        List<ItemStack> drops = silkTouch && !block.getDrops(tool).isEmpty()
                ? List.of(new ItemStack(type))
                : applyFortune(block, tool, fortuneLevel);

        Inventory inventory = player.getInventory();
        if (!hasInventorySpace(inventory, drops)) return;

        event.setDropItems(false);
        block.setType(Material.AIR);
        depositItems(inventory, player, drops);
    }

    private boolean hasInventorySpace(Inventory inventory, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            int remaining = drop.getAmount();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (slot != null && slot.isSimilar(drop) && slot.getAmount() < slot.getMaxStackSize()) {
                    int space = slot.getMaxStackSize() - slot.getAmount();
                    if (space >= remaining) return true;
                } else if (slot == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void depositItems(Inventory inventory, Player player, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            int amountToStore = drop.getAmount();
            boolean stored = false;

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (slot != null && slot.isSimilar(drop) && slot.getAmount() < slot.getMaxStackSize()) {
                    int space = slot.getMaxStackSize() - slot.getAmount();
                    int transfer = Math.min(space, amountToStore);
                    slot.setAmount(slot.getAmount() + transfer);
                    amountToStore -= transfer;
                    if (amountToStore <= 0) {
                        stored = true;
                        break;
                    }
                }
            }

            if (!stored && amountToStore > 0) {
                int emptySlot = inventory.firstEmpty();
                if (emptySlot != -1) {
                    ItemStack copy = drop.clone();
                    copy.setAmount(amountToStore);
                    inventory.setItem(emptySlot, copy);
                } else {
                    ItemStack leftover = drop.clone();
                    leftover.setAmount(amountToStore);
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
    }

    private List<ItemStack> applyFortune(Block block, ItemStack tool, int level) {
        List<ItemStack> originalDrops = new ArrayList<>(block.getDrops(tool));
        if (originalDrops.isEmpty() || level <= 0) return originalDrops;

        List<ItemStack> result = new ArrayList<>();
        Random random = new Random();

        for (ItemStack item : originalDrops) {
            int multiplier = Math.max(1, random.nextInt(level + 2));
            ItemStack copy = item.clone();
            copy.setAmount(item.getAmount() * multiplier);
            result.add(copy);
        }
        return result;
    }
}
