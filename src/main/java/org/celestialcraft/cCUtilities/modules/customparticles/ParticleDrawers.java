package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.*;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

final class ParticleDrawers {
    private ParticleDrawers() {}

    /** Paper-only TPS; safe fallback to 1.0 on non-Paper. */
    static double tpsScalar() {
        try {
            double tps = Bukkit.getServer().getTPS()[0]; // last 1m on Paper
            if (tps >= 19.5) return 1.0;
            if (tps >= 18.0) return 0.8;
            if (tps >= 16.0) return 0.6;
            return 0.4;
        } catch (Throwable ignored) {
            return 1.0; // Spigot/vanilla fallback
        }
    }

    /** Hide if invisible/vanished/spectator/dead/invalid. */
    static boolean shouldRender(Player p) {
        if (p == null || !p.isOnline() || !p.isValid() || p.isDead()) return false;
        if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
        try {
            if (p.hasMetadata("vanished")) return false;
            if (p.hasMetadata("essentials:vanished")) return false;
            if (p.hasMetadata("supervanish:vanished")) return false;
        } catch (Throwable ignored) {}
        return p.getGameMode() != org.bukkit.GameMode.SPECTATOR;
    }

    // ---- individual effects (scale work by s) ----

    static void drawFlameRing(Player p, double s) {
        if (!shouldRender(p)) return;
        Location loc = p.getLocation();
        World w = loc.getWorld();
        if (w == null) return;

        double y = loc.getY() + 0.1;
        double r = 0.8 * s;
        int points = Math.max(8, (int) Math.round(24 * s));
        double cx = loc.getX(), cz = loc.getZ();
        for (int i = 0; i < points; i++) {
            double a = (Math.PI * 2 * i) / points;
            double x = cx + Math.cos(a) * r;
            double z = cz + Math.sin(a) * r;
            w.spawnParticle(Particle.FLAME, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    static void drawWaterSplash(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(2, (int) Math.round(6 * s));
        w.spawnParticle(Particle.SPLASH, l.getX(), l.getY() + 0.1, l.getZ(),
                count, 0.35 * s, 0.05, 0.35 * s, 0.02);
    }

    static void drawDragonFire(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(2, (int) Math.round(8 * s));
        double spread = 0.25 * s;
        w.spawnParticle(Particle.DRAGON_BREATH, l.getX(), l.getY() + 0.6, l.getZ(),
                count, spread, spread, spread, 0.02);
    }

    static void drawHeartTrail(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = (s >= 0.9 ? 1 : 0);
        if (count > 0) w.spawnParticle(Particle.HEART, l.getX(), l.getY() + 1.2, l.getZ(), count, 0, 0, 0, 0);
    }

    static void drawLightningArc(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(4, (int) Math.round(10 * s));
        w.spawnParticle(Particle.ELECTRIC_SPARK, l.getX(), l.getY() + 0.9, l.getZ(),
                count, 0.5 * s, 0.2, 0.5 * s, 0);
    }

    static void drawStarTrail(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(1, (int) Math.round(2 * s));
        w.spawnParticle(Particle.END_ROD, l.getX(), l.getY() + 1.0, l.getZ(),
                count, 0.1 * s, 0.1 * s, 0.1 * s, 0.01);
    }

    static void drawRainbowSpiral(Player p, double s) {
        if (!shouldRender(p)) return;
        Location base = p.getLocation().add(0, 0.2, 0);
        World w = base.getWorld();
        if (w == null) return;

        double r = 0.6 * s;
        int steps = Math.max(6, (int) Math.round(12 * s));
        for (int i = 0; i < steps; i++) {
            double a = (Math.PI * 2 * i) / steps;
            double x = base.getX() + Math.cos(a) * r;
            double y = base.getY() + (i * 0.08 * s);
            double z = base.getZ() + Math.sin(a) * r;
            float hue = (float) i / steps;
            java.awt.Color awt = java.awt.Color.getHSBColor(hue, 1f, 1f);
            Color c = Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
            DustOptions dust = new DustOptions(c, 1.2f);
            w.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
        }
    }

    static void drawWingBurst(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(2, (int) Math.round(8 * s));
        w.spawnParticle(Particle.CLOUD, l.getX(), l.getY() + 1.0, l.getZ(),
                count, 0.6 * s, 0.15, 0.6 * s, 0.02);
    }

    static void drawCherryWings(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(2, (int) Math.round(4 * s));
        w.spawnParticle(Particle.CHERRY_LEAVES, l.getX(), l.getY() + 1.2, l.getZ(),
                count, 0.5 * s, 0.2, 0.5 * s, 0.02);
    }

    static void drawCloudAura(Player p, double s) {
        if (!shouldRender(p)) return;
        Location l = p.getLocation();
        World w = l.getWorld();
        if (w == null) return;

        int count = Math.max(2, (int) Math.round(4 * s));
        w.spawnParticle(Particle.CLOUD, l.getX(), l.getY() + 0.5, l.getZ(),
                count, 0.5 * s, 0.2, 0.5 * s, 0.01);
    }

    private static final Material[] LEAF_MATS = new Material[] {
            Material.PALE_OAK_LEAVES, // 1.21 – if not present on your jar, it’s fine; guarded below
            Material.OAK_LEAVES,
            Material.BIRCH_LEAVES,
            Material.ACACIA_LEAVES,
            Material.DARK_OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES,
            Material.MANGROVE_LEAVES,
            Material.CHERRY_LEAVES
    };

    private static BlockData randomLeafData() {
        // Bias a bit towards the first half for more “pale/oak” vibes
        int idx = ThreadLocalRandom.current().nextInt(LEAF_MATS.length);
        Material m = LEAF_MATS[idx];
        try {
            return Bukkit.createBlockData(m);
        } catch (Throwable ignored) {
            return Bukkit.createBlockData(Material.OAK_LEAVES);
        }
    }

    /** Soft fluttering leaf trail that follows the player as they move. */
    static void drawAutumnLeaves(Player p, double s) {
        if (!shouldRender(p)) return;
        Location base = p.getLocation().add(0, 0.1, 0);
        World w = base.getWorld();
        if (w == null) return;

        // Scale with s (tpsScalar), keep it subtle to avoid spam
        int puffs = Math.max(4, (int)Math.round(6 * s));
        double spread = 0.35 * s;

        for (int i = 0; i < puffs; i++) {
            double ox = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
            double oy = ThreadLocalRandom.current().nextDouble() * 0.20;
            double oz = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
            w.spawnParticle(Particle.BLOCK, base.getX(), base.getY(), base.getZ(),
                    1, ox, oy, oz, 0, randomLeafData());
        }
    }

    static void drawBloodyIcosphere(Player p, double s) {
        if (!shouldRender(p)) return;

        Location loc = p.getLocation().add(0, 1.0, 0); // torso height
        World w = loc.getWorld();
        if (w == null) return;

        int points = 30; // number of particles
        double radius = 2.0 * s; // hollow sphere radius

        for (int i = 0; i < points; i++) {
            double theta = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * ThreadLocalRandom.current().nextDouble() - 1);

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            w.spawnParticle(
                    Particle.DUST,
                    loc.clone().add(x, y, z),
                    1,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.2f)
            );
        }
    }
}
