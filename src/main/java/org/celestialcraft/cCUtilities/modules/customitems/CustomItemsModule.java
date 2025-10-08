package org.celestialcraft.cCUtilities.modules.customitems;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.celestialcraft.cCUtilities.listeners.CustomEffectsListener;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;
import org.celestialcraft.cCUtilities.modules.modulemanager.ModuleManager;

public class CustomItemsModule implements Module {

    private final JavaPlugin plugin;
    private boolean enabled = false;

    // Hold a single instance for SpiderBackpackItem to share across registration and listener usage
    private SpiderBackpackItem spiderBackpackItem;
    private WitchDisguiseItem witchDisguiseItem;


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
        // Pass the single SpiderBackpackItem instance into the listener so it keeps consistent state
        Bukkit.getPluginManager().registerEvents(new CustomEffectsListener(spiderBackpackItem), plugin);
        Bukkit.getPluginManager().registerEvents(witchDisguiseItem, plugin);
    }

    public void registerAll(JavaPlugin plugin) {
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
        CustomItemRegistry.register(new MobPacifierItem(plugin));
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
        CustomItemRegistry.register(new DayFlyTokenItem());
        CustomItemRegistry.register(new AxeOfLifeItem());
        CustomItemRegistry.register(new BabyConverterItem());
        CustomItemRegistry.register(new BowOfLevitationItem());
        CustomItemRegistry.register(new CelestialTransporterItem());
        CustomItemRegistry.register(new DoubleXpSwordItem());
        CustomItemRegistry.register(new EnderbowItem());
        CustomItemRegistry.register(new FairytaleArmorItem());
        CustomItemRegistry.register(new FantasyPickaxeItem());
        CustomItemRegistry.register(new FlowerWandItem());
        CustomItemRegistry.register(new IceStaffItem());
        CustomItemRegistry.register(new InstaMagicBroomItem());
        CustomItemRegistry.register(new MushroomWandItem());
        CustomItemRegistry.register(new PeaShooterItem());
        CustomItemRegistry.register(new PoisonedAppleItem());
        CustomItemRegistry.register(new RandomSpawnEggItem());
        CustomItemRegistry.register(new RandomFairytaleArmorItem());
        CustomItemRegistry.register(new FairytaleHatBoxItem());
        CustomItemRegistry.register(new RedRidingHoodHelmItem());
        CustomItemRegistry.register(new ReusableFlyTokenItem());
        CustomItemRegistry.register(new TerracottaPickaxeItem());
        CustomItemRegistry.register(new RubySlippersItem());
        CustomItemRegistry.register(new SandSmelterItem());
        CustomItemRegistry.register(new SourCitrusItem());
        CustomItemRegistry.register(new UnstripperItem());
        CustomItemRegistry.register(new WaxyEffectItem());
        CustomItemRegistry.register(new XpStorageItem());
        CustomItemRegistry.register(new ReapersScythe());
        CustomItemRegistry.register(new JumpScareWand());

        // IMPORTANT: Use the single SpiderBackpackItem instance for registry and later listener
        spiderBackpackItem = new SpiderBackpackItem(plugin);
        CustomItemRegistry.register(spiderBackpackItem);

        witchDisguiseItem = new WitchDisguiseItem(plugin);
        CustomItemRegistry.register(witchDisguiseItem);


        CustomItemRegistry.register(new BatWingsItem());
        CustomItemRegistry.register(new BlackstonePickaxeItem());
        CustomItemRegistry.register(new ReapersScythe());
        CustomItemRegistry.register(new SavannaBiomeWand());
        CustomItemRegistry.register(new TrickOrTreatItem());
        CustomItemRegistry.register(new DemonTridentItem());
        CustomItemRegistry.register(new UltimateInvisibilityWand());
        CustomItemRegistry.register(new HalloweenRankTokenItem());
        CustomItemRegistry.register(new HalloweenHatBoxItem());
        CustomItemRegistry.register(new HalloweenPetBoxItem());
        CustomItemRegistry.register(new LilJackPetTokenItem());
        CustomItemRegistry.register(new LilGrimPetTokenItem());
        CustomItemRegistry.register(new BroomStickPetTokenItem());

        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            CustomItemRegistry.register(new CapybaraSpawnEggItem());
        } else {
            plugin.getLogger().warning("LuckPerms not found — Capybara Spawn Egg item disabled.");
        }
    }
}

