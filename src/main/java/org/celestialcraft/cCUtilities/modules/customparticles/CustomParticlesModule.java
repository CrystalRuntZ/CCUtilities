package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.CCUtilities;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchant;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;

public class CustomParticlesModule implements Module {

    private boolean enabled = false;
    // Keep this static so it survives disable/enable without double-registering enchants.
    private static volatile boolean ENCHANTS_REGISTERED = false;

    @Override
    public void enable() {
        if (enabled) return;
        JavaPlugin plugin = CCUtilities.getInstance();

        // Give ParticleManager a plugin instance (safe to call multiple times)
        ParticleManager.init(plugin);

        // Register listener once per enable
        CustomParticlesListener listener = new CustomParticlesListener();
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        // Refresh all currently online players so effects apply immediately
        Bukkit.getOnlinePlayers().forEach(listener::refresh);

        // Register enchants only once per JVM lifetime to avoid duplicate-id exceptions
        if (!ENCHANTS_REGISTERED) {
            registerEnchantSafe(new CloudAuraEnchant(), plugin);
            registerEnchantSafe(new DragonFireEnchant(), plugin);
            registerEnchantSafe(new FlameRingEnchant(), plugin);
            registerEnchantSafe(new HeartTrailEnchant(), plugin);
            registerEnchantSafe(new LightningArcEnchant(), plugin);
            registerEnchantSafe(new RainbowParticlesEnchant(), plugin);
            registerEnchantSafe(new StarTrailEnchant(), plugin);
            registerEnchantSafe(new WaterSplashEnchant(), plugin);
            registerEnchantSafe(new WingBurstEnchant(), plugin);
            registerEnchantSafe(new CherryWingEnchant(), plugin);
            ENCHANTS_REGISTERED = true;
            plugin.getLogger().info("[CustomParticles] Enchants registered.");
        } else {
            plugin.getLogger().info("[CustomParticles] Enchants already registered; skipping.");
        }

        enabled = true;
        plugin.getLogger().info("[CustomParticles] Module enabled. Listener registered & players refreshed.");
    }

    @Override
    public void disable() {
        enabled = false;
        // Do NOT unregister enchants; keep them global. Just stop rendering.
        ParticleManager.shutdown();
        JavaPlugin plugin = CCUtilities.getInstance();
        plugin.getLogger().info("[CustomParticles] Module disabled. Scheduler stopped.");
    }

    @Override public boolean isEnabled() { return enabled; }
    @Override public String getName() { return "customparticles"; }

    // --- helpers ---
    private void registerEnchantSafe(CustomEnchant enchant, JavaPlugin plugin) {
        try {
            CustomEnchantRegistry.register(enchant);
        } catch (IllegalArgumentException dup) {
            // Id already registered — safe to ignore and continue
            plugin.getLogger().warning("[CustomParticles] Enchant already registered: " + enchant.getIdentifier());
        } catch (Throwable t) {
            plugin.getLogger().severe("[CustomParticles] Failed to register enchant " + enchant.getIdentifier() + ": " + t.getMessage());
        }
    }
}
