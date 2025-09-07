package org.celestialcraft.cCUtilities.modules.customenchants;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.listeners.CustomEnchantAnvilStackingListener;
import org.celestialcraft.cCUtilities.listeners.CustomEnchantEffectListener;
import org.celestialcraft.cCUtilities.modules.customparticles.*;
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
        // --- Existing combat/utility enchants you already had ---
        CustomEnchantRegistry.register(new StrengthOfTheEndEnchant());

        LuckEnchant luck = new LuckEnchant();
        luck.setPlugin(plugin);
        CustomEnchantRegistry.register(luck);

        // --- Particle Enchants (all anvil-compatible) ---
        CustomEnchantRegistry.register(new FlameRingEnchant());
        CustomEnchantRegistry.register(new HeartTrailEnchant());
        CustomEnchantRegistry.register(new RainbowParticlesEnchant());
        CustomEnchantRegistry.register(new WingBurstEnchant());
        CustomEnchantRegistry.register(new StarTrailEnchant());
        CustomEnchantRegistry.register(new CloudAuraEnchant());
        CustomEnchantRegistry.register(new DragonFireEnchant());
        CustomEnchantRegistry.register(new WaterSplashEnchant());
        CustomEnchantRegistry.register(new LightningArcEnchant());
        CustomEnchantRegistry.register(new CherryWingEnchant());

        // --- Your existing registrations ---
        TunnelingEnchant tunneling = new TunnelingEnchant();
        CustomEnchantRegistry.register(tunneling);

        ZombieRepellantEnchant zombieRepellant = new ZombieRepellantEnchant();
        CustomEnchantRegistry.register(zombieRepellant);

        BeheadingEnchant beheading = new BeheadingEnchant();
        CustomEnchantRegistry.register(beheading);

        CustomEnchantRegistry.register(new InvisibilityEffectEnchant());

        // --- NEW enchants from your request ---
        CustomEnchantRegistry.register(new PhantomRepellantEnchant());     // any item
        CustomEnchantRegistry.register(new MidasPlightEnchant());          // pickaxes
        CustomEnchantRegistry.register(new GreenThumbEnchant());           // hoes
        CustomEnchantRegistry.register(new AutosmeltEnchant());            // pickaxes
        CustomEnchantRegistry.register(new DragonbreathImmunityEnchant()); // any item
        CustomEnchantRegistry.register(new UnbreakableEnchant());          // any item
        CustomEnchantRegistry.register(new DoubleXPEnchant());             // pickaxes/swords/axes
        CustomEnchantRegistry.register(new SpeedTwoEnchant());             // any item
        CustomEnchantRegistry.register(new JumpBoostTwoEnchant());         // any item
        CustomEnchantRegistry.register(new SlownessImmunityEnchant());     // any item
        CustomEnchantRegistry.register(new SatietyEffectEnchant());        // any item
        CustomEnchantRegistry.register(new MobSlayerCounterEnchant());     // swords/axes
        CustomEnchantRegistry.register(new OreMinerCounterEnchant());      // pickaxes
        CustomEnchantRegistry.register(new NightVisionEnchant());          // any item
        CustomEnchantRegistry.register(new RainbowGlowEffectEnchant());
        CustomEnchantRegistry.register(new RandomEfficiencyEnchant());
        CustomEnchantRegistry.register(new DestroyerOfTheEndEnchant());
        CustomEnchantRegistry.register(new CyberParticlesEnchant());
        CustomEnchantRegistry.register(new CripplingEnchant());
        CustomEnchantRegistry.register(new BleedEnchant());
        CustomEnchantRegistry.register(new ProtectionOfTheEndEnchant());
        CustomEnchantRegistry.register(new EndermanPleaserEnchant());
        CustomEnchantRegistry.register(new LevitationImmunityEnchant());
        CustomEnchantRegistry.register(new VoidSafetyEnchant());

        // --- Listeners ---
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new CustomEnchantAnvilStackingListener(), plugin);
        pm.registerEvents(new CustomEnchantEffectListener(), plugin);
        // NOTE: some of the new enchants respond to BlockBreak/EntityDeath/BlockExp/ItemDamage events.
        // If you don't already have a forwarder for those, I can add a fan-out listener next.
    }
}
