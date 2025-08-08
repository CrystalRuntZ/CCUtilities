package org.celestialcraft.cCUtilities.modules.customitems.portal;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.celestialcraft.cCUtilities.CCUtilities;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class PortalManager {

    private static final Map<UUID, PortalData> activePortals = new HashMap<>();
    private static final Map<UUID, List<PortalData>> portalsByOwner = new HashMap<>();
    private static final File file = new File(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("CCUtilities")).getDataFolder(), "data/portals.json");

    public static void load() {
        activePortals.clear();
        portalsByOwner.clear();
        if (!file.exists()) return;

        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (String key : root.keySet()) {
                UUID gunId = UUID.fromString(key);
                JsonObject obj = root.getAsJsonObject(key);
                UUID ownerId = UUID.fromString(obj.get("owner").getAsString());

                PortalData data = new PortalData(gunId, ownerId);
                if (obj.has("left")) data.setLeft(deserializeLocation(obj.getAsJsonObject("left")));
                if (obj.has("right")) data.setRight(deserializeLocation(obj.getAsJsonObject("right")));

                activePortals.put(gunId, data);
                portalsByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(data);
            }
        } catch (IOException e) {
            CCUtilities.getInstance().getLogger().log(Level.SEVERE, "[PortalManager] Failed to load portals.json", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (PortalData data : activePortals.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("owner", data.getOwnerId().toString());
            if (data.getLeft() != null) obj.add("left", serializeLocation(data.getLeft()));
            if (data.getRight() != null) obj.add("right", serializeLocation(data.getRight()));
            root.add(data.getGunId().toString(), obj);
        }

        try {
            File parentDir = file.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                CCUtilities.getInstance().getLogger().warning("[PortalManager] Failed to create parent directory for portals.json");
                return;
            }

            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (IOException e) {
            CCUtilities.getInstance().getLogger().log(Level.SEVERE, "[PortalManager] Failed to save portals.json", e);
        }
    }

    public static PortalData getOrCreate(UUID gunId, UUID ownerId) {
        return activePortals.computeIfAbsent(gunId, id -> {
            PortalData data = new PortalData(gunId, ownerId);
            portalsByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(data);
            return data;
        });
    }

    public static PortalData get(UUID gunId) {
        return activePortals.get(gunId);
    }

    @SuppressWarnings("unused")
    public static Collection<PortalData> getAllPortals() {
        return activePortals.values();
    }

    public static List<PortalData> getPortalsForPlayer(UUID ownerId) {
        return portalsByOwner.getOrDefault(ownerId, Collections.emptyList());
    }

    private static JsonObject serializeLocation(Location loc) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", loc.getWorld().getName());
        obj.addProperty("x", loc.getX());
        obj.addProperty("y", loc.getY());
        obj.addProperty("z", loc.getZ());
        return obj;
    }

    private static Location deserializeLocation(JsonObject obj) {
        World world = Bukkit.getWorld(obj.get("world").getAsString());
        double x = obj.get("x").getAsDouble();
        double y = obj.get("y").getAsDouble();
        double z = obj.get("z").getAsDouble();
        return new Location(world, x, y, z);
    }
}
