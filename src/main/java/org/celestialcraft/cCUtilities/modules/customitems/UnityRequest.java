package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record UnityRequest(UUID from, ItemStack originalItem) {}
