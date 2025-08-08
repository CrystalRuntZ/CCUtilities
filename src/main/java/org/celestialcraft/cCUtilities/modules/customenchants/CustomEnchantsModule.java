package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.listeners.CustomEnchantAnvilListener;
import org.celestialcraft.cCUtilities.listeners.CustomEnchantEffectListener;
import org.celestialcraft.cCUtilities.modules.customparticles.FlameRingEnchant;
import org.celestialcraft.cCUtilities.modules.customparticles.HeartTrailEnchant;
import org.celestialcraft.cCUtilities.modules.customparticles.RainbowParticlesEnchant;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomEnchantsModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public CustomEnchantsModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;
        registerAll(plugin);
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "customenchants";
    }

    public static void registerAll(JavaPlugin plugin) {
        StrengthOfTheEndEnchant strength = new StrengthOfTheEndEnchant();
        strength.setPlugin(plugin);
        CustomEnchantRegistry.register(strength);

        DestroyerOfTheEndEnchant destroyer = new DestroyerOfTheEndEnchant();
        destroyer.setPlugin(plugin);
        CustomEnchantRegistry.register(destroyer);

        LuckEnchant luck = new LuckEnchant();
        luck.setPlugin(plugin);
        CustomEnchantRegistry.register(luck);

        ProtectionOfTheEndEnchant protection = new ProtectionOfTheEndEnchant();
        protection.setPlugin(plugin);
        CustomEnchantRegistry.register(protection);

        BleedEnchant bleed = new BleedEnchant();
        bleed.setPlugin(plugin);
        CustomEnchantRegistry.register(bleed);

        CripplingEnchant crippling = new CripplingEnchant();
        crippling.setPlugin(plugin);
        CustomEnchantRegistry.register(crippling);

        CyberParticlesEnchant cyber = new CyberParticlesEnchant();
        cyber.setPlugin(plugin);
        CustomEnchantRegistry.register(cyber);

        FlameRingEnchant flameRing = new FlameRingEnchant();
        flameRing.setPlugin(plugin);
        CustomEnchantRegistry.register(flameRing);

        HeartTrailEnchant heartTrail = new HeartTrailEnchant();
        heartTrail.setPlugin(plugin);
        CustomEnchantRegistry.register(heartTrail);

        RainbowParticlesEnchant rainbowParticles = new RainbowParticlesEnchant();
        rainbowParticles.setPlugin(plugin);
        CustomEnchantRegistry.register(rainbowParticles);

        TunnelingEnchant tunneling = new TunnelingEnchant();
        CustomEnchantRegistry.register(tunneling);

        ZombieRepellantEnchant zombieRepellant = new ZombieRepellantEnchant();
        CustomEnchantRegistry.register(zombieRepellant);

        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new CustomEnchantAnvilListener(), plugin);
        pm.registerEvents(new CustomEnchantEffectListener(), plugin);
    }

}
