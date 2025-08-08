package org.celestialcraft.cCUtilities.modules.modulemanager;

import java.util.*;

public class ModuleManager {
    private static final Map<String, Module> modules = new HashMap<>();

    public static void register(Module module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public static boolean enable(String name) {
        Module module = modules.get(name.toLowerCase());
        if (module == null || module.isEnabled()) return false;
        module.enable();
        ModulesConfig.markEnabled(name);
        return true;
    }

    public static boolean disable(String name) {
        Module module = modules.get(name.toLowerCase());
        if (module == null || !module.isEnabled()) return false;
        module.disable();
        ModulesConfig.markDisabled(name);
        return true;
    }

    public static Collection<Module> getModules() {
        return modules.values();
    }

    public static boolean isEnabled(String name) {
        Module module = modules.get(name.toLowerCase());
        return module != null && module.isEnabled();
    }

    public static void reload() {
        ModulesConfig.reload();
    }
}
