package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public interface CustomEnchant {
    String getIdentifier();
    boolean appliesTo(ItemStack item);
    boolean hasEnchant(ItemStack item);
    void applyEffect(EntityDamageByEntityEvent event);

    /** Legacy/standard apply path. */
    ItemStack applyTo(ItemStack item);

    /** Displayed lore line for this enchant (e.g., "&7My Enchant"). */
    String getLoreLine();

    /**
     * Opt-in: return true if this enchant is allowed to be applied to ANY item
     * (including normally non-enchantable items like paper/sugarcane).
     * Default is false to preserve current behavior.
     */
    default boolean canApplyToAnyItem() { return false; }

    /**
     * Force-apply path used by anvil/merging code when this enchant is allowed on any item.
     * Default delegates to the legacy applyTo(...) to remain backward-compatible.
     * Enchants that set PDC or need special handling can override this.
     */
    default ItemStack applyTo(ItemStack item, boolean force) {
        return applyTo(item);
    }

    // --- Optional lifecycle hooks (unchanged) ---
    default void onJoin(PlayerJoinEvent event) {}
    default void onQuit(PlayerQuitEvent event) {}
    default void onHeld(PlayerItemHeldEvent event) {}
    default void onHandSwap(Player player) {}
    default void onShootBow(EntityShootBowEvent event) {}
    default void onPlayerMove(Player player) {}
    default void onEntityTarget(EntityTargetLivingEntityEvent event) {}
    default void onTick(Player player, ItemStack item) {}
    default void onMove(PlayerMoveEvent event) {}
    default void onArmorEquip(Player player) {}
    default void onArmorUnequip(Player player) {}
    default void onPlayerDeath(PlayerDeathEvent event) {}
    default void onBlockBreak(BlockBreakEvent event) {}
    default void onBlockExp(BlockExpEvent event) {}
    default void onEntityDeath(EntityDeathEvent event) {}
    default void onItemDamage(PlayerItemDamageEvent event) {}
}
