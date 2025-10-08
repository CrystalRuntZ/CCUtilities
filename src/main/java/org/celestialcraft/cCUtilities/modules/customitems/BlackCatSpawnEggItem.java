package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.util.LoreUtil;
import org.celestialcraft.cCUtilities.utils.ClaimUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackCatSpawnEggItem implements CustomItem, Listener {

    private static final String LORE_IDENTIFIER = "§7Black Cat Spawn Egg";
    private static final int MAX_CATS = 10;

    private static final File STORAGE_FILE = new File(CCUtilities.getInstance().getDataFolder(), "black_cats.yml");
    private static final FileConfiguration STORAGE = YamlConfiguration.loadConfiguration(STORAGE_FILE);

    private static final Map<UUID, List<Cat>> playerCats = new ConcurrentHashMap<>();

    private final Map<UUID, BukkitTask> catProtectionTasks = new ConcurrentHashMap<>();

    private static final NamespacedKey OWNER_KEY = new NamespacedKey(CCUtilities.getInstance(), "black_cat_owner");

    @Override
    public String getIdentifier() {
        return "black_cat_spawn_egg";
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!matches(item)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<Cat> cats = playerCats.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));

        if (cats.size() >= MAX_CATS) {
            player.sendActionBar(Component.text("You have reached the maximum number of black cats.").color(TextColor.color(0xFF0000)));
            event.setCancelled(true);
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendActionBar(Component.text("Please look at a valid block to spawn the cat."));
            event.setCancelled(true);
            return;
        }

        Location spawnLocation = targetBlock.getLocation().add(0.5, 1, 0.5); // Spawn slightly above ground

        // Use canBuild for claim check on exact spawn location
        if (!ClaimUtils.canBuild(player, spawnLocation)) {
            player.sendActionBar(Component.text("You can only spawn your black cat within your own claim.").color(TextColor.color(0xFF0000)));
            event.setCancelled(true);
            return;
        }

        Cat cat = spawnBlackCat(player, spawnLocation);
        cats.add(cat);
        saveCat(cat);

        // Consume one egg from player's hand
        assert item != null;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }

        event.setCancelled(true);
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != Material.CAT_SPAWN_EGG) return false;
        return LoreUtil.itemHasLore(item, LORE_IDENTIFIER);
    }

    private Cat spawnBlackCat(Player owner, Location loc) {
        Cat cat = (Cat) owner.getWorld().spawnEntity(loc, EntityType.CAT);
        cat.setOwner(owner);

        cat.setCatType(Cat.Type.BLACK); // force cat to be black

        cat.customName(Component.text("🎃" + owner.getName() + "'s Black Cat🎃").color(TextColor.fromHexString("#c1adfe")));
        cat.setCustomNameVisible(true);
        cat.setCollarColor(org.bukkit.DyeColor.BLACK);

        // Store owner UUID in PersistentDataContainer for future reference
        cat.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(CCUtilities.getInstance(), () -> {
            if (cat.isDead()) {
                cancelProtectionTask(cat.getUniqueId());
                return;
            }
            cat.getNearbyEntities(15, 15, 15).forEach(e -> {
                if (e instanceof org.bukkit.entity.Monster mob) {
                    mob.setTarget(null);
                    Vector away = mob.getLocation().toVector().subtract(cat.getLocation().toVector());
                    if (away.lengthSquared() > 0.01) {
                        mob.setVelocity(away.normalize().multiply(0.5));
                    }
                }
            });
        }, 0L, 20L);

        catProtectionTasks.put(cat.getUniqueId(), task);
        return cat;
    }

    private void cancelProtectionTask(UUID catUUID) {
        BukkitTask task = catProtectionTasks.remove(catUUID);
        if (task != null) task.cancel();
    }

    public static void loadCats() {
        for (String key : STORAGE.getKeys(false)) {
            String ownerStr = STORAGE.getString(key + ".owner");
            Location loc = STORAGE.getSerializable(key + ".location", Location.class);
            if (ownerStr == null || loc == null) continue;

            UUID ownerUUID = UUID.fromString(ownerStr);
            Player player = Bukkit.getPlayer(ownerUUID);
            if (player == null) continue;

            List<Cat> cats = playerCats.computeIfAbsent(ownerUUID, k -> Collections.synchronizedList(new ArrayList<>()));
            if (cats.size() >= MAX_CATS) continue;

            Cat cat = new BlackCatSpawnEggItem().spawnBlackCat(player, loc);
            cats.add(cat);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        List<Cat> cats = playerCats.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));

        for (String key : STORAGE.getKeys(false)) {
            if (key == null) continue;
            if (!key.equalsIgnoreCase(uuid.toString())) continue;

            Location loc = STORAGE.getSerializable(key + ".location", Location.class);
            if (loc == null) continue;

            if (cats.size() < MAX_CATS) {
                Cat cat = spawnBlackCat(event.getPlayer(), loc);
                cats.add(cat);
            }
        }
    }

    public void onCatDeath(Cat cat) {
        if (cat == null) return;
        cancelProtectionTask(cat.getUniqueId());
        playerCats.forEach((uuid, cats) -> {
            if (cats.remove(cat)) {
                removeCatFromStorage(cat);
            }
        });
    }

    private static void saveCat(Cat cat) {
        if (cat == null || cat.getOwner() == null) return;
        String key = cat.getUniqueId().toString();
        STORAGE.set(key + ".owner", cat.getOwner().getUniqueId().toString());
        STORAGE.set(key + ".location", cat.getLocation());
        try {
            STORAGE.save(STORAGE_FILE);
        } catch (IOException e) {
            CCUtilities.getInstance().getLogger().warning("Failed to save black cats storage file: " + e.getMessage());
        }
    }

    private static void removeCatFromStorage(Cat cat) {
        if (cat == null) return;
        STORAGE.set(cat.getUniqueId().toString(), null);
        try {
            STORAGE.save(STORAGE_FILE);
        } catch (IOException e) {
            CCUtilities.getInstance().getLogger().warning("Failed to save black cats storage file: " + e.getMessage());
        }
    }
}
