package org.celestialcraft.cCUtilities.modules.ced;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.celestialcraft.cCUtilities.MessageConfig;

import java.util.*;

public class DragonManager {
    private final JavaPlugin plugin;
    private final DragonConfig config;
    private final DamageTracker damageTracker;
    private final BossBarHandler bossBarHandler;

    private EnderDragon activeDragon;
    private DragonType activeDragonType;
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

                    Bukkit.getLogger().info("[CED] Dragon died. UUID: " + deadDragon.getUniqueId());
                    Bukkit.getLogger().info("[CED] Active dragon UUID: " + getActiveDragonId());

                    Bukkit.getPluginManager().callEvent(new DragonKillEvent(deadDragon, type));
                    killSpawnedMobs();
                }
            }
        }, plugin);
    }

    public boolean isDragonActive() {
        return activeDragon != null && activeDragon.isValid() && !activeDragon.isDead();
    }

    public void registerMob(LivingEntity entity) {
        spawnedMobs.add(entity.getUniqueId());
    }

    public void killSpawnedMobs() {
        for (UUID uuid : spawnedMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                living.remove();
            }
        }
        spawnedMobs.clear();
    }

    public void spawnDragon(DragonType type) {
        if (isDragonActive()) return;

        World endWorld = Bukkit.getWorld(plugin.getConfig().getString("spawn-world", "wild_the_end"));
        if (endWorld == null) {
            plugin.getLogger().warning("End world not found.");
            return;
        }

        Location spawnLoc = new Location(endWorld, 0, 64, 0);
        EnderDragon dragon = (EnderDragon) endWorld.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);

        this.activeDragon = dragon;
        this.activeDragonType = type;

        dragon.setPhase(EnderDragon.Phase.CIRCLING);
        dragon.setAI(true);

        // ✅ Get name from messages.yml first, fallback to enum default
        String rawName = MessageConfig.get("ced.dragon-names." + type.name());
        final String nameString = (rawName == null || rawName.isBlank()) ? type.getDefaultFancyName() : rawName;

        // Colored name for entity
        Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(nameString);
        dragon.customName(nameComponent);
        dragon.setCustomNameVisible(true);

        // ✅ Boss bar color debug
        BarColor color = config.getBossBarColor(type);
        Bukkit.getLogger().info("[CED] Boss bar color for " + type.name() + " = " + color);

// Plain text for boss bar display name (color handled separately)
        Component plainName = Component.text(PlainTextComponentSerializer.plainText().serialize(nameComponent));

// ✅ Get health config values once
        double base = config.getBaseHealth(type);
        double perPlayer = config.getHealthPerPlayer(type);

// Show boss bar with debug values
        bossBarHandler.showBossBar(type, plainName, color, base, perPlayer);

// ✅ Custom health logic — set twice to ensure persistence
        int online = Math.max(1, Bukkit.getOnlinePlayers().size());
        double virtualMaxHealth = base + online * perPlayer;

// Debug health calculation
        Bukkit.getLogger().info("[CED] Base health: " + base);
        Bukkit.getLogger().info("[CED] Health per player: " + perPlayer);
        Bukkit.getLogger().info("[CED] Online players: " + online);
        Bukkit.getLogger().info("[CED] Calculated virtual max health: " + virtualMaxHealth);

        Runnable setHealthTask = () -> {
            if (!dragon.isValid()) return;
            AttributeInstance attr = dragon.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(virtualMaxHealth);
            dragon.setHealth(virtualMaxHealth);
        };

        Bukkit.getScheduler().runTaskLater(plugin, setHealthTask, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, setHealthTask, 10L);



        damageTracker.register(dragon, virtualMaxHealth);

        // Boss bar updater loop
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

        // ✅ Broadcast spawn message with MiniMessage for template, legacy for name
        String spawnTemplate = MessageConfig.get("ced.spawn");
        Component spawnMessage = mini.deserialize(spawnTemplate)
                .replaceText(builder -> builder.matchLiteral("{name}").replacement(nameComponent));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(spawnMessage));

        DragonUtils.setDragonType(dragon, type);
        this.lastActivityTime = System.currentTimeMillis();

        plugin.getServer().getPluginManager().callEvent(new DragonSpawnEvent(dragon, type));

        // ✅ Vanish timer with same name formatting fix
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
                    killActiveDragon();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public boolean killActiveDragon() {
        if (activeDragon == null || !activeDragon.isValid()) return false;

        World world = activeDragon.getWorld();
        activeDragon.remove();
        DragonUtils.clearEntitiesOnMainIsland(world);
        killSpawnedMobs();

        activeDragon = null;
        activeDragonType = null;

        if (bossBarUpdater != null) bossBarUpdater.cancel();
        bossBarHandler.hideAllBossBars();

        return true;
    }

    public EnderDragon getActiveDragon() {
        return activeDragon;
    }

    public UUID getActiveDragonId() {
        return activeDragon != null ? activeDragon.getUniqueId() : null;
    }

    public DragonType getActiveDragonType() {
        return activeDragonType;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    public DragonConfig getConfig() {
        return config;
    }

    public void reload() {
        this.config.reload();
    }

    public void clearActiveDragon() {
        activeDragon = null;
        activeDragonType = null;
        if (bossBarUpdater != null) bossBarUpdater.cancel();
        bossBarHandler.hideAllBossBars();
        killSpawnedMobs();
    }
}
