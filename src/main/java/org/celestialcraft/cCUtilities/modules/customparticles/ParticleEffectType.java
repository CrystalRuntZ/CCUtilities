package org.celestialcraft.cCUtilities.modules.customparticles;

import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public enum ParticleEffectType {
    FLAME_RING    (ParticleDrawers::drawFlameRing),
    HEART_TRAIL   (ParticleDrawers::drawHeartTrail),
    RAINBOW_SPIRAL(ParticleDrawers::drawRainbowSpiral),
    WING_BURST    (ParticleDrawers::drawWingBurst),
    STAR_TRAIL    (ParticleDrawers::drawStarTrail),
    CLOUD_AURA    (ParticleDrawers::drawCloudAura),
    DRAGON_FIRE   (ParticleDrawers::drawDragonFire),
    WATER_SPLASH  (ParticleDrawers::drawWaterSplash),
    LIGHTNING_ARC (ParticleDrawers::drawLightningArc),
    CHERRY_WINGS  (ParticleDrawers::drawCherryWings),
    AUTUMN_LEAVES (ParticleDrawers::drawAutumnLeaves),
    BLOODY_ICOSPHERE(ParticleDrawers::drawBloodyIcosphere);

    private final BiConsumer<Player, Double> renderer;

    ParticleEffectType(BiConsumer<Player, Double> renderer) {
        this.renderer = renderer;
    }

    public void render(Player p, double scalar) {
        renderer.accept(p, scalar);
    }
}
