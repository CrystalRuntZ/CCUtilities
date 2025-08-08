package org.celestialcraft.cCUtilities.modules.customenchants;

import java.util.*;

public class CustomEnchantRegistry {
    private static final Map<String, CustomEnchant> enchants = new HashMap<>();

    public static void register(CustomEnchant enchant) {
        enchants.put(enchant.getIdentifier(), enchant);
    }

    public static Collection<CustomEnchant> getAll() {
        return enchants.values();
    }

    public static Optional<CustomEnchant> getById(String id) {
        return Optional.ofNullable(enchants.get(id));
    }
}
