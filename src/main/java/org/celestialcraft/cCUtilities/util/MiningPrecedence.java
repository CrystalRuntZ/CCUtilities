package org.celestialcraft.cCUtilities.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import static org.celestialcraft.cCUtilities.util.ItemChecks.*;

public final class MiningPrecedence {
    private MiningPrecedence() {}

    public enum ToolClass { PICKAXE, AXE, SHOVEL, HOE, SHEARS, SWORD, NONE }

    /** Best-effort classify a tool from its material. */
    public static ToolClass classifyTool(ItemStack tool) {
        if (isPickaxe(tool)) return ToolClass.PICKAXE;
        if (isAxe(tool))     return ToolClass.AXE;
        if (isShovel(tool))  return ToolClass.SHOVEL;
        if (isHoe(tool))     return ToolClass.HOE;
        if (isSword(tool))   return ToolClass.SWORD;
        if (anyNonAir(tool) && tool.getType() == Material.SHEARS) return ToolClass.SHEARS;
        return ToolClass.NONE;
    }

    /** Quick skip list for AOE miners (tunneling/vein). */
    public static boolean isUnbreakableOrShouldSkip(Block b) {
        if (b == null) return true;
        Material m = b.getType();
        return switch (m) {
            case BEDROCK, REINFORCED_DEEPSLATE, BARRIER, END_PORTAL_FRAME,
                 END_PORTAL, NETHER_PORTAL, COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK,
                 STRUCTURE_BLOCK, JIGSAW -> true;
            default -> false;
        };
    }

    /** Heuristic: which tool is appropriate for the block (approximate vanilla tags). */
    public static ToolClass preferredTool(Block b) {
        if (b == null) return ToolClass.NONE;
        String n = b.getType().name();

        // Pickaxe: ores, stone/deepslate variants, concrete, terracotta, metal blocks, glass-like
        if (n.contains("ORE") || n.contains("DEEPSLATE") || n.contains("STONE") || n.contains("NETHERRACK")
                || n.endsWith("_TERRACOTTA") || n.endsWith("_GLASS") || n.endsWith("_GLAZED_TERRACOTTA")
                || n.endsWith("_COPPER_BLOCK") || n.endsWith("_INGOT_BLOCK") || n.endsWith("_BLOCK")
                || n.contains("OBSIDIAN") || n.contains("BLACKSTONE") || n.contains("BASALT")) {
            return ToolClass.PICKAXE;
        }

        // Shovel: dirt/sand/gravel/clay/snow/concrete_powder
        if (n.contains("DIRT") || n.contains("SAND") || n.contains("GRAVEL") || n.contains("CLAY")
                || n.contains("SOUL_SAND") || n.contains("SOUL_SOIL")
                || n.contains("SNOW") || n.contains("POWDER_SNOW")
                || n.contains("CONCRETE_POWDER") || n.contains("MUD")) {
            return ToolClass.SHOVEL;
        }

        // Axe: wood logs, planks, stems, hyphae, barrels, bookshelves, pumpkins/melons
        if (n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")
                || n.endsWith("_PLANKS") || n.contains("BARREL") || n.contains("BOOKSHELF")
                || n.contains("PUMPKIN") || n.contains("MELON")) {
            return ToolClass.AXE;
        }

        // Hoe: leaves, wart blocks, hay, sponges?
        if (n.endsWith("_LEAVES") || n.endsWith("_WART_BLOCK") || n.contains("HAY_BLOCK") || n.contains("SPONGE")) {
            return ToolClass.HOE;
        }

        return ToolClass.NONE;
    }

    /** True if the provided tool class is appropriate for mining this block. */
    public static boolean isPreferredTool(Block b, ItemStack tool) {
        ToolClass have = classifyTool(tool);
        ToolClass want = preferredTool(b);
        if (want == ToolClass.NONE) return true; // neutral: allow any
        return have == want;
    }
}
