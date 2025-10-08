package org.celestialcraft.cCUtilities.modules.customenchants;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.celestialcraft.cCUtilities.util.ItemChecks;
import org.celestialcraft.cCUtilities.util.LoreUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RainbowGlowEffectEnchant implements CustomEnchant {

    private static final String RAW_LORE = "&7Rainbow Glow Effect";
    private static final NamedTextColor[] COLORS = {
            NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW,
            NamedTextColor.GREEN, NamedTextColor.BLUE, NamedTextColor.LIGHT_PURPLE
    };
    private static final String TEAM_PREFIX = "ccu_rgb_";

    private final Map<UUID, Integer> taskIds  = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> colorIdx = new ConcurrentHashMap<>();
    private JavaPlugin plugin;

    @Override public String getIdentifier() { return "rainbow_glow_effect"; }
    @Override public String getLoreLine()   { return RAW_LORE; }

    @Override
    public boolean appliesTo(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public boolean canApplyToAnyItem() { return true; }

    @Override
    public boolean hasEnchant(ItemStack item) {
        return LoreUtil.itemHasLore(item, RAW_LORE);
    }

    @Override public void applyEffect(EntityDamageByEntityEvent event) { /* none */ }

    @Override public void onHeld(PlayerItemHeldEvent event) { reconcile(event.getPlayer()); }
    @Override public void onHandSwap(Player player)         { reconcile(player); }
    @Override public void onPlayerMove(Player player)       { reconcile(player); }
    @Override public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) { reconcile(event.getPlayer()); }
    @Override public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { stopFor(event.getPlayer()); }

    @Override
    public ItemStack applyTo(ItemStack item) { return applyTo(item, false); }

    @Override
    public ItemStack applyTo(ItemStack item, boolean force) {
        if (item == null) return null;
        if (!force && !appliesTo(item)) return item;
        LoreUtil.ensureLoreAtTop(item, RAW_LORE);
        return item;
    }

    private void reconcile(Player p) {
        if (ItemChecks.hasAnywhere(p, this::hasEnchant)) startFor(p);
        else                                             stopFor(p);
    }

    private void startFor(Player p) {
        if (plugin == null) return; // not initialized
        UUID id = p.getUniqueId();
        if (taskIds.containsKey(id)) { ensureTeamAndGlowing(p); return; }

        colorIdx.putIfAbsent(id, 0);
        ensureTeamAndGlowing(p);

        int task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !ItemChecks.hasAnywhere(p, this::hasEnchant)) { stopFor(p); return; }
            int idx = (colorIdx.getOrDefault(id, 0) + 1) % COLORS.length;
            colorIdx.put(id, idx);
            setTeamColorPortable(getOrCreateTeam(p), COLORS[idx]);
            if (!p.isGlowing()) p.setGlowing(true);
        }, 0L, 15L).getTaskId();

        taskIds.put(id, task);
    }

    private void stopFor(Player p) {
        UUID id = p.getUniqueId();
        Integer task = taskIds.remove(id);
        if (task != null) Bukkit.getScheduler().cancelTask(task);
        p.setGlowing(false);
        removeFromTeam(p);
    }

    private void ensureTeamAndGlowing(Player p) {
        Team t = getOrCreateTeam(p);
        addToTeamIfMissing(t, p);
        int idx = colorIdx.getOrDefault(p.getUniqueId(), 0) % COLORS.length;
        setTeamColorPortable(t, COLORS[idx]);
        p.setGlowing(true);
    }

    private Team getOrCreateTeam(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = TEAM_PREFIX + p.getUniqueId().toString().replace("-", "");
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        return team;
    }

    private void addToTeamIfMissing(Team t, Player p) {
        String entry = p.getName();
        if (!t.hasEntry(entry)) t.addEntry(entry);
    }

    private void removeFromTeam(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = TEAM_PREFIX + p.getUniqueId().toString().replace("-", "");
        Team team = board.getTeam(teamName);
        if (team != null) team.removeEntry(p.getName());
    }

    /** Paper: team.color(NamedTextColor); Spigot legacy: setColor(ChatColor) via reflection. */
    private void setTeamColorPortable(Team team, NamedTextColor color) {
        if (team == null || color == null) return;

        try {
            Method m = team.getClass().getMethod("color", NamedTextColor.class);
            m.invoke(team, color);
            return;
        } catch (Throwable ignored) {}

        try {
            Method m2 = team.getClass().getMethod("setColor", NamedTextColor.class);
            m2.invoke(team, color);
            return;
        } catch (Throwable ignored) {}

        try {
            String chatName = chatColorName(color);
            Class<?> chatColorClz = Class.forName("org.bukkit.ChatColor");
            Method valueOf = chatColorClz.getMethod("valueOf", String.class);
            Object chatColorObj = valueOf.invoke(null, chatName);
            Method legacySetter = team.getClass().getMethod("setColor", chatColorClz);
            legacySetter.invoke(team, chatColorObj);
        } catch (Throwable ignored) { /* default color */ }
    }

    private String chatColorName(NamedTextColor c) {
        if (c == NamedTextColor.BLACK) return "BLACK";
        if (c == NamedTextColor.DARK_BLUE) return "DARK_BLUE";
        if (c == NamedTextColor.DARK_GREEN) return "DARK_GREEN";
        if (c == NamedTextColor.DARK_AQUA) return "DARK_AQUA";
        if (c == NamedTextColor.DARK_RED) return "DARK_RED";
        if (c == NamedTextColor.DARK_PURPLE) return "DARK_PURPLE";
        if (c == NamedTextColor.GOLD) return "GOLD";
        if (c == NamedTextColor.GRAY) return "GRAY";
        if (c == NamedTextColor.DARK_GRAY) return "DARK_GRAY";
        if (c == NamedTextColor.BLUE) return "BLUE";
        if (c == NamedTextColor.GREEN) return "GREEN";
        if (c == NamedTextColor.AQUA) return "AQUA";
        if (c == NamedTextColor.RED) return "RED";
        if (c == NamedTextColor.LIGHT_PURPLE) return "LIGHT_PURPLE";
        if (c == NamedTextColor.YELLOW) return "YELLOW";
        return "WHITE";
    }

    public void setPlugin(JavaPlugin plugin) { this.plugin = plugin; }
}
