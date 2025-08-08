package org.celestialcraft.cCUtilities.modules.ced;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

public enum DragonType {
    FIRE("fire", "Solvarian, Dragon of the Sun"),
    ICE("ice", "Zephyrael, Warden of Frost"),
    VOID("void", "Nyxarion, Devourer of Light"),
    STORM("storm", "Tempestros, Eye of the Maelstrom"),
    EARTH("earth", "Gaiadon, Shaper of Stone"),
    SHADOW("shadow", "Umbrarok, The Hollow Gaze"),
    LIGHT("light", "Aurelos, Bringer of Dawn"),
    TOXIC("toxic", "Venomir, Blight of the Void"),
    ARCANE("arcane", "Mystavon, Seer of Secrets"),
    SAND("sand", "Duneborn, The Shifting Tyrant"),
    BLOOD("blood", "Crimsonas, Leech of Life"),
    PLAGUE("plague", "Rotwyrm, Pestilence Incarnate"),
    CRYSTAL("crystal", "Glaceryl, Prism of Eternity"),
    AETHER("aether", "Elyndra, Whisper in the Wind"),
    HELLFIRE("hellfire", "Cindergon, The Inferno Maw"),
    CELESTIAL("celestial", "Astralith, Herald of Stars");

    private final String configKey;
    private final String defaultFancyName;

    DragonType(String configKey, String defaultFancyName) {
        this.configKey = configKey;
        this.defaultFancyName = defaultFancyName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDefaultFancyName() {
        return defaultFancyName;
    }

    public static Optional<DragonType> fromName(String input) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(input))
                .findFirst();
    }

    public static DragonType getRandom() {
        DragonType[] values = values();
        return values[new Random().nextInt(values.length)];
    }

    public String getDisplayName() {
        String lowercase = name().toLowerCase();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1) + " Dragon";
    }
}
