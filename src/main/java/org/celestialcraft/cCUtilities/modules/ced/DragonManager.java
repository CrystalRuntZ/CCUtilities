package org.celestialcraft.cCUtilities.modules.ced;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DragonManager {
    private final JavaPlugin plugin;
    private final DragonConfig config;
    private final DamageTracker damageTracker;
    private final BossBarHandler bossBarHandler;

    private EnderDragon activeDragon;
    private DragonType activeDragonType;
    private UUID currentFightId; // stable ID for the whole fight
    private BukkitRunnable bossBarUpdater;
    private long lastActivityTime = 0L;

    private final Set<UUID> spawnedMobs = new HashSet<>();
    private static final MiniMessage mini = MiniMessage.miniMessage();

    public DragonManager(JavaPlugin plugin, DragonConfig config, DamageTracker damageTracker, BossBarHandler bossBarHandler) {
        this.plugin = plugin;
        this.config = config;
        this.damageTracker = damageTracker;
        this.bossBarHandler = bossBarHandler;

        AutoDragonSpawner.start(plugin, this);

        // Inline listener for activity + death hooks
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityDamage(EntityDamageEvent event) {
                if (activeDragon != null && activeDragon.equals(event.getEntity())) {
                    lastActivityTime = System.currentTimeMillis();
                }
            }

            @EventHandler
            public void onEntityDeath(EntityDeathEvent event) {
                if (activeDragon != null && activeDragon.equals(event.getEntity())) {
                    EnderDragon deadDragon = activeDragon;
                    DragonType type = activeDragonType;

                    // Ensure we purge everything tied to this fight
                    purgeFightMobs();

                    Bukkit.getPluginManager().callEvent(new DragonKillEvent(deadDragon, type));
                }
            }
        }, plugin);
    }

    public boolean isDragonActive() {
        return activeDragon != null && activeDragon.isValid() && !activeDragon.isDead();
    }

    /** Register any mob spawned by the fight; also PDC-tag it with the current fightId for guaranteed cleanup. */
    public void registerMob(LivingEntity entity) {
        if (entity == null) return;
        spawnedMobs.add(entity.getUniqueId());
        if (currentFightId != null) {
            DragonUtils.tagAsFightMob(entity, currentFightId);
        }
    }

    /** Remove both explicitly tracked mobs and any stray tagged mobs in the End world. */
    public void killSpawnedMobs() {
        // 1) Remove explicitly tracked
        for (UUID uuid : new HashSet<>(spawnedMobs)) {
            var e = Bukkit.getEntity(uuid);
            if (e instanceof LivingEntity living) {
                living.remove();
            }
        }
        spawnedMobs.clear();

        // 2) Also purge by tag across the End world
        World endWorld = getEndWorld();
        if (endWorld != null && currentFightId != null) {
            DragonUtils.clearFightMobs(endWorld, currentFightId);
            // Optionally also ensure island subset is clean
            DragonUtils.clearTaggedOnMainIsland(endWorld, currentFightId);
        }
    }

    public void spawnDragon(DragonType type) {
        if (isDragonActive()) return;

        World endWorld = getEndWorld();
        if (endWorld == null) return;

        // Optional pre-fight sweep
        if (plugin.getConfig().getBoolean("ced.clear-island-on-fight-start", false)) {
            DragonUtils.clearEntitiesOnMainIsland(endWorld);
        }

        Location spawnLoc = new Location(endWorld, 0, 64, 0);
        EnderDragon dragon = (EnderDragon) endWorld.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);

        this.activeDragon = dragon;
        this.activeDragonType = type;
        this.currentFightId = dragon.getUniqueId(); // set fight id up-front

        dragon.setPhase(EnderDragon.Phase.CIRCLING);
        dragon.setAI(true);

        String rawName = MessageConfig.get("ced.dragon-names." + type.name());
        final String nameString = (rawName == null || rawName.isBlank()) ? type.getDefaultFancyName() : rawName;

        Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(nameString);
        dragon.customName(nameComponent);
        dragon.setCustomNameVisible(true);

        BarColor color = config.getBossBarColor(type);
        Component plainName = Component.text(PlainTextComponentSerializer.plainText().serialize(nameComponent));

        double base = config.getBaseHealth(type);
        double perPlayer = config.getHealthPerPlayer(type);
        bossBarHandler.showBossBar(type, plainName, color, base, perPlayer);

        int online = Math.max(1, Bukkit.getOnlinePlayers().size());
        double virtualMaxHealth = base + online * perPlayer;

        Runnable setHealthTask = () -> {
            if (!dragon.isValid()) return;
            AttributeInstance attr = dragon.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(virtualMaxHealth);
            dragon.setHealth(virtualMaxHealth);
        };
        Bukkit.getScheduler().runTaskLater(plugin, setHealthTask, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, setHealthTask, 10L);

        damageTracker.register(dragon, virtualMaxHealth);

        if (bossBarUpdater != null) bossBarUpdater.cancel();
        bossBarUpdater = new BukkitRunnable() {
            private int tickCounter = 0;
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    this.cancel();
                    bossBarHandler.hideAllBossBars();
                    return;
                }
                double progress = damageTracker.getProgress(dragon);
                bossBarHandler.updateProgress(type, progress);
                if (++tickCounter % 40 == 0) {
                    bossBarHandler.updateVisiblePlayers(type);
                }
            }
        };
        bossBarUpdater.runTaskTimer(plugin, 0L, 2L);

        String spawnTemplate = MessageConfig.get("ced.spawn");
        Component spawnMessage = mini.deserialize(spawnTemplate)
                .replaceText(builder -> builder.matchLiteral("{name}").replacement(nameComponent));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(spawnMessage));

        DragonUtils.setDragonType(dragon, type);
        this.lastActivityTime = System.currentTimeMillis();

        plugin.getServer().getPluginManager().callEvent(new DragonSpawnEvent(dragon, type));

        // Vanish-on-idle watchdog
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!dragon.isValid() || dragon.isDead()) {
                    this.cancel();
                    return;
                }
                if (System.currentTimeMillis() - lastActivityTime > 300_000L) {
                    String vanishTemplate = MessageConfig.get("ced.vanish");
                    Component vanishMsg = mini.deserialize(vanishTemplate)
                            .replaceText(builder -> builder.matchLiteral("{name}").replacement(nameComponent));
                    Bukkit.getServer().sendMessage(vanishMsg);
                    killActiveDragon(); // will purge by tag too
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public boolean killActiveDragon() {
        if (activeDragon == null || !activeDragon.isValid()) {
            // Still attempt purge by last fightId to clean leftovers
            purgeFightMobs();

            // Optional broad broom after fight ends even if dragon already gone
            World endWorld = getEndWorld();
            if (endWorld != null && plugin.getConfig().getBoolean("ced.clear-island-on-fight-end", true)) {
                DragonUtils.clearEntitiesOnMainIsland(endWorld);
            }
            return false;
        }

        World world = activeDragon.getWorld();

        // Remove dragon
        activeDragon.remove();

        // Targeted purge by tag (global in End) + island sweep for tagged remnants
        purgeFightMobs();
        DragonUtils.clearTaggedOnMainIsland(world, null); // also clean any tagged from older/orphan fights

        // Optional broad broom for untagged leftovers
        if (plugin.getConfig().getBoolean("ced.clear-island-on-fight-end", true)) {
            DragonUtils.clearEntitiesOnMainIsland(world);
        }

        // Housekeeping
        if (bossBarUpdater != null) bossBarUpdater.cancel();
        bossBarHandler.hideAllBossBars();

        activeDragon = null;
        activeDragonType = null;
        currentFightId = null;
        return true;
    }

    /** Purge all mobs associated with the current fightId, plus the tracked set. */
    private void purgeFightMobs() {
        killSpawnedMobs();

        // Safety: when no fight is active, remove all tagged mobs from any fight
        World endWorld = getEndWorld();
        if (!isDragonActive() && endWorld != null) {
            DragonUtils.clearAllFightMobs(endWorld);
        }
    }

    public EnderDragon getActiveDragon() { return activeDragon; }
    public UUID getActiveDragonId()      { return activeDragon != null ? activeDragon.getUniqueId() : null; }
    public DragonType getActiveDragonType() { return activeDragonType; }
    public DamageTracker getDamageTracker() { return damageTracker; }
    public DragonConfig getConfig() { return config; }

    public void reload() { this.config.reload(); }

    public void clearActiveDragon() {
        // Purge by tag before forgetting the fight
        purgeFightMobs();
        activeDragon = null;
        activeDragonType = null;
        currentFightId = null;
        if (bossBarUpdater != null) bossBarUpdater.cancel();
        bossBarHandler.hideAllBossBars();
    }

    // ---- helpers ----

    private World getEndWorld() {
        String worldName = plugin.getConfig().getString("spawn-world", "wild_the_end");
        return Bukkit.getWorld(worldName);
    }
}
