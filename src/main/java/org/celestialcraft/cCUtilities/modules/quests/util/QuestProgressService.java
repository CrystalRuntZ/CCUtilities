package org.celestialcraft.cCUtilities.modules.quests.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundle;
import org.celestialcraft.cCUtilities.modules.quests.bundle.WeeklyBundleStorage;
import org.celestialcraft.cCUtilities.modules.quests.model.Quest;
import org.celestialcraft.cCUtilities.modules.quests.model.QuestType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestProgressService {
    private final Plugin plugin;
    private final WeeklyBundleStorage storage;

    public QuestProgressService(Plugin plugin) {
        this.plugin = plugin;
        this.storage = new WeeklyBundleStorage(plugin);
    }

    /* -----------------------
       Core progress updates
       ----------------------- */

    public boolean addProgress(Player p, QuestType type, int amount) {
        return addProgress(p, type, null, amount);
    }

    public boolean addProgress(Player p, QuestType type, String targetFilter, int amount) {
        UUID bundleId = null;
        int slot = -1;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            UUID id = WeeklyQuestItemFactory.getBundleId(plugin, contents[i]);
            if (id != null) { bundleId = id; slot = i; break; }
        }
        if (bundleId == null) return false;

        WeeklyBundle bundle = storage.load(bundleId);
        if (bundle == null) return false;

        boolean changed = false;
        List<Quest> changedQuests = new ArrayList<>();

        for (Quest q : bundle.getQuests()) {
            if (q.getType() != type) continue;

            // If this quest has a target (like a specific mob/block), compare flexibly.
            String qTarget = q.getTargetItem();
            boolean targetOk = (targetFilter == null) || qTarget == null || equalsNormalized(qTarget, targetFilter);
            if (!targetOk) continue;

            int before = q.getProgress();
            int after  = Math.min(q.getTarget(), Math.max(0, before + amount));
            if (after != before) {
                q.setProgress(after);
                changed = true;
                changedQuests.add(q);
            }
        }
        if (!changed) return false;

        storage.save(bundle);

        final int s = slot;
        final ItemStack updated = WeeklyQuestItemFactory.syncLore(plugin, p.getInventory().getItem(s), bundle);
        plugin.getServer().getScheduler().runTask(plugin, () -> p.getInventory().setItem(s, updated));

        for (Quest q : changedQuests) {
            LoreUtils.sendProgressActionBar(p, q);
        }
        return true;
    }

    /* -----------------------
       Admin / maintenance helpers
       ----------------------- */

    /** Remove any weekly quest "paper" in the player's inventory that points to a missing or foreign bundle.
     *  Returns the number of items removed. Valid items get their lore synced in-place. */
    public int removeInvalidWeeklyItems(Player p) {
        int removed = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            UUID id = WeeklyQuestItemFactory.getBundleId(plugin, it);
            if (id == null) continue;

            WeeklyBundle bundle = storage.load(id);
            if (bundle == null || !bundle.getOwner().equals(p.getUniqueId())) {
                // Missing bundle or owned by someone else -> remove the item
                p.getInventory().setItem(i, null);
                removed++;
                continue;
            }

            // Keep valid bundle items, but ensure lore is up-to-date
            ItemStack updated = WeeklyQuestItemFactory.syncLore(plugin, it, bundle);
            if (updated != it) {
                p.getInventory().setItem(i, updated);
            }
        }
        return removed;
    }

    /** Delete all stored bundles owned by the given player. Returns the number of bundles purged. */
    public int purgeOwner(UUID owner) {
        return storage.purgeOwner(owner);
    }

    /* -----------------------
       Flexible target matching
       ----------------------- */

    // Compare after normalization + plural-tolerant checks
    private boolean equalsNormalized(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        return na.equals(nb) || matchesFlex(na, nb) || matchesFlex(nb, na);
    }

    private boolean matchesFlex(String a, String b) {
        if (a.equals(b)) return true;

        // simple trailing 's'
        if (a.endsWith("s") && a.length() > 1) {
            if (baseMinus(a, 1).equals(b)) return true;
        }

        // trailing 'es' (witches -> witch, foxes -> fox)
        if (a.endsWith("es") && a.length() > 2) {
            if (baseMinus(a, 2).equals(b)) return true;
        }

        // 'ies' -> 'ie' and 'y' (zombies -> zombie; accept both)
        if (a.endsWith("ies") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "ie").equals(b) || (base3 + "y").equals(b)) return true;
        }

        // 'ves' -> 'f' / 'fe' (wolves -> wolf)
        if (a.endsWith("ves") && a.length() > 3) {
            String base3 = baseMinus(a, 3);
            if ((base3 + "f").equals(b) || (base3 + "fe").equals(b)) return true;
        }

        // Also allow pluralizing toward b (reverse handled by calling matchesFlex the other way)
        return (a + "s").equals(b) || (a + "es").equals(b);
    }

    private String baseMinus(String s, int suffixLen) {
        return s.substring(0, s.length() - suffixLen);
    }

    // Lowercase, remove namespace, normalize separators
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();

        // strip namespace if present (e.g., "minecraft:husk" -> "husk")
        int colon = s.indexOf(':');
        if (colon >= 0 && colon + 1 < s.length()) {
            s = s.substring(colon + 1);
        }

        // unify separators to underscore
        s = s.replace('-', '_').replace(' ', '_');
        return s;
    }
}
