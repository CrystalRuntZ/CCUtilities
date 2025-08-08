package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.listeners.CustomItemEffectListener;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomItemsModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    public CustomItemsModule(JavaPlugin plugin) {
        this.plugin = plugin;
        ModuleManager.register(this);
    }

    @Override
    public void enable() {
        if (enabled) return;

        registerAll(plugin);
        registerListeners();

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
        return "customitems";
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CustomItemEffectListener(), plugin);
    }

    public static void registerAll(JavaPlugin plugin) {
        CustomItemRegistry.register(new ArchaeologyShovelItem());
        CustomItemRegistry.register(new GuqinItem());
        CustomItemRegistry.register(new InfiniteEnderPearlItem());
        CustomItemRegistry.register(new LemurLeaperItem());
        CustomItemRegistry.register(new MapleStrippingAxeItem());
        CustomItemRegistry.register(new MercyUltimateItem(plugin));
        CustomItemRegistry.register(new MoaiItem());
        CustomItemRegistry.register(new RailPlacerItem());
        CustomItemRegistry.register(new SombreroItem());
        CustomItemRegistry.register(new SurrenderItem());
        CustomItemRegistry.register(new TryzubTridentItem());
        CustomItemRegistry.register(new UnityItem());
        CustomItemRegistry.register(new VegemiteItem());
        CustomItemRegistry.register(new WarHorseItem());
        CustomItemRegistry.register(new FlippingWandItem());
        CustomItemRegistry.register(new BrazilianBrewItem(plugin));
        CustomItemRegistry.register(new DugrasDaggerItem());
        CustomItemRegistry.register(new DragonSpawningTokenItem());
        CustomItemRegistry.register(new PotionOfTheEndItem(plugin));
        CustomItemRegistry.register(new CelestialFishingRod());
        CustomItemRegistry.register(new BedrockPickaxeItem());
        CustomItemRegistry.register(new BerserkerItem());
        CustomItemRegistry.register(new CelestialBucketItem());
        CustomItemRegistry.register(new CelestialLocatorItem());
        CustomItemRegistry.register(new CelestialTeleporterItem());
        CustomItemRegistry.register(new CondenseCommandItem());
        CustomItemRegistry.register(new CriticalChestplateItem());
        CustomItemRegistry.register(new CyberCottonCandyItem());
        CustomItemRegistry.register(new CyberStorageItem(plugin));
        CustomItemRegistry.register(new DyeShooterItem());
        CustomItemRegistry.register(new ForestBiomeWand());
        CustomItemRegistry.register(new FrostyChestplateItem());
        CustomItemRegistry.register(new GwynnbleiddItem());
        CustomItemRegistry.register(new IceKingsStaffItem());
        CustomItemRegistry.register(new LavaforgedBootsItem());
        CustomItemRegistry.register(new LeapingMaceItem());
        CustomItemRegistry.register(new LifestealSwordItem());
        CustomItemRegistry.register(new MantisBladeItem());
        CustomItemRegistry.register(new MobPacifierItem());
        CustomItemRegistry.register(new MoltenCorePickaxeItem());
        CustomItemRegistry.register(new MultitoolItem());
        CustomItemRegistry.register(new ParticleWandItem());
        CustomItemRegistry.register(new PhantomBattleAxeItem());
        CustomItemRegistry.register(new PhoenixChestplateItem());
        CustomItemRegistry.register(new PixieDustItem());
        CustomItemRegistry.register(new PocketSandItem());
        CustomItemRegistry.register(new PortalGunItem());
        CustomItemRegistry.register(new PotOfGreedItem());
        CustomItemRegistry.register(new PropulsionBoosterItem());
        CustomItemRegistry.register(new RabidBunnyHatItem());
        CustomItemRegistry.register(new RandomMagicalChestplateItem());
        CustomItemRegistry.register(new RandomPotionEffectItem());
        CustomItemRegistry.register(new ReallocatorItem());
        CustomItemRegistry.register(new SandmansShovelItem());
        CustomItemRegistry.register(new ShearsOfDestructionItem());
        CustomItemRegistry.register(new SilkSpawnerPickItem());
        CustomItemRegistry.register(new SilkTouchSpawnerItem());
        CustomItemRegistry.register(new SirensPushItem());
        CustomItemRegistry.register(new SlipperyShoesItem());
        CustomItemRegistry.register(new StrikebreakerItem());
        CustomItemRegistry.register(new ThermalHelmetItem());
        CustomItemRegistry.register(new ThunderbowItem());
        CustomItemRegistry.register(new ToxicChestplateItem());
        CustomItemRegistry.register(new VoidPickaxeItem());
        CustomItemRegistry.register(new WindStaffItem());
        CustomItemRegistry.register(new RailGunItem());

        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            CustomItemRegistry.register(new CapybaraSpawnEggItem());
        } else {
            plugin.getLogger().warning("LuckPerms not found — Capybara Spawn Egg item disabled.");
        }
    }
}
