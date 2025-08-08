package org.celestialcraft.cCUtilities.modules.modulemanager;

public interface Module {
    String getName();
    void enable();
    void disable();
    boolean isEnabled();
}
