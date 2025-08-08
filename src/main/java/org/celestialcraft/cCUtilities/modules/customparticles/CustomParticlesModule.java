package org.celestialcraft.cCUtilities.modules.customparticles;

import org.celestialcraft.cCUtilities.modules.customenchants.CustomEnchantRegistry;
import org.celestialcraft.cCUtilities.modules.modulemanager.Module;

public class CustomParticlesModule implements Module {

    private boolean enabled = false;

    @Override
    public void enable() {
        if (enabled) return;
        CustomEnchantRegistry.register(new FlameRingEnchant());
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
        return "customparticles";
    }
}
