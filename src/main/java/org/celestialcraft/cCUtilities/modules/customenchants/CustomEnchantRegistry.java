package org.celestialcraft.cCUtilities.modules.customenchants;

import java.util.*;

public class CustomEnchantRegistry {
    private static final Map<String, CustomEnchant> enchants = new HashMap<>();

    public static void register(CustomEnchant enchant) {
        String id = enchant.getIdentifier().toLowerCase(Locale.ROOT);
        if (enchants.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate CustomEnchant id: " + id);
        }
        enchants.put(id, enchant);
    }

    public static Set<String> getAllIdentifiers() {
        return new HashSet<>(enchants.keySet());
    }

    public static Collection<CustomEnchant> getAll() {
        return Collections.unmodifiableCollection(enchants.values());
    }

    public static Optional<CustomEnchant> getById(String id) {
        return Optional.ofNullable(enchants.get(id.toLowerCase(Locale.ROOT)));
    }

    public static void clear() { enchants.clear(); } // if you ever hot-reload
}
