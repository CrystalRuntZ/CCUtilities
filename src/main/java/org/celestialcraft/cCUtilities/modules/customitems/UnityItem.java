package org.celestialcraft.cCUtilities.modules.customitems;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class UnityItem implements CustomItem {
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private final Map<UUID, Long> messageCooldownMap = new HashMap<>();
    private static final NamespacedKey uuidKey = NamespacedKey.minecraft("unity_cooldown");

    private static final String GRAY = "§7";
    private static final String ACCENT = toLegacyHex();

    @Override
    public String getIdentifier() {
        return "unity_item";
    }

    private boolean isInCombat(Player player) {
        return player.hasMetadata("inCombat") || player.hasMetadata("pvptag");
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta() || item.getItemMeta().lore() == null)
            return false;
        List<Component> lore = item.getItemMeta().lore();
        return lore != null && !lore.isEmpty() && serializer.serialize(lore.getFirst()).equals("§7Unity Item");
    }

    // Fired on some servers
    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) return;
        handleUnitePrompt(player, target);
    }

    // Fired on other servers/impls (precise hitbox)
    @Override
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) return;
        handleUnitePrompt(player, target);
    }

    private void handleUnitePrompt(Player player, Player target) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        List<Component> lore = Objects.requireNonNull(item.getItemMeta().lore());
        if (lore.size() < 3) return;

        String line2 = serializer.serialize(lore.get(1)).replace("§7Partner:", "").trim();
        String line3 = serializer.serialize(lore.get(2)).replace("§7Partner:", "").trim();

        boolean isUnlinked = line2.isEmpty() || line3.isEmpty();
        if (!isUnlinked) return;

        long now = System.currentTimeMillis();
        long lastMessage = messageCooldownMap.getOrDefault(target.getUniqueId(), 0L);
        if (now - lastMessage < 3000) return;

        Component prompt = Component.text("☆ ", TextColor.color(0xc1adfe))
                .append(Component.text(player.getName() + " would like to unite with you, do you accept? ", NamedTextColor.GRAY))
                .append(Component.text("[YES]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/unityaccept " + player.getUniqueId())))
                .append(Component.space())
                .append(Component.text("[NO]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/unitydeny " + player.getUniqueId())));

        target.sendMessage(prompt);
        messageCooldownMap.put(target.getUniqueId(), now);
        UnityRequestManager.requests.put(target.getUniqueId(), new UnityRequest(player.getUniqueId(), item.clone()));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (isInCombat(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!matches(item)) return;

        List<Component> lore = Objects.requireNonNull(item.getItemMeta().lore());
        if (lore.size() < 3) return;

        String raw2 = serializer.serialize(lore.get(1)).replace("§7Partner:", "").trim();
        String raw3 = serializer.serialize(lore.get(2)).replace("§7Partner:", "").trim();
        String line2 = raw2.replaceAll("§x(§[0-9a-fA-F]){6}", "").replaceAll("§.", "");
        String line3 = raw3.replaceAll("§x(§[0-9a-fA-F]){6}", "").replaceAll("§.", "");

        String partnerName = line2.equalsIgnoreCase(player.getName()) ? line3 :
                line3.equalsIgnoreCase(player.getName()) ? line2 : null;

        if (partnerName == null) return;
        if (!isPartneredWith(lore, player.getName(), partnerName)) return;

        Player partner = Bukkit.getPlayerExact(partnerName);
        if (partner == null || partner.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.SPECTATOR) return;

        UUID itemUUID = getOrSetItemUUID(item);
        long lastUse = cooldownMap.getOrDefault(itemUUID, 0L);
        if (System.currentTimeMillis() - lastUse < 60_000) {
            player.sendActionBar(Component.text("§7Your Unity Item is on cooldown."));
            return;
        }

        cooldownMap.put(itemUUID, System.currentTimeMillis());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "teleport " + player.getName() + " " + partnerName);
    }

    private boolean isPartneredWith(List<Component> lore, String p1, String p2) {
        Set<String> partners = new HashSet<>();
        for (Component line : lore) {
            String raw = serializer.serialize(line);
            String stripped = raw
                    .replace("§7Partner:", "")
                    .replaceAll("§x(§[0-9a-fA-F]){6}", "")
                    .replaceAll("§.", "")
                    .trim();
            if (!stripped.isEmpty()) partners.add(stripped);
        }
        return partners.contains(p1) && partners.contains(p2);
    }

    private UUID getOrSetItemUUID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String raw = container.get(uuidKey, PersistentDataType.STRING);
        UUID result;
        try {
            result = raw != null ? UUID.fromString(raw) : UUID.randomUUID();
        } catch (Exception e) {
            result = UUID.randomUUID();
        }
        container.set(uuidKey, PersistentDataType.STRING, result.toString());
        item.setItemMeta(meta);
        return result;
    }

    public static ItemStack createLinkedItem(String player1, String player2, ItemStack original) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

        // Keep existing lore, only fill lines 2 & 3 (index 1,2)
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        while (lore.size() < 3) lore.add(Component.empty());

        // Write partner lines in legacy colors and FORCE no italics
        Component l2 = LEGACY.deserialize(GRAY + "Partner: " + ACCENT + player1)
                .decoration(TextDecoration.ITALIC, false);
        Component l3 = LEGACY.deserialize(GRAY + "Partner: " + ACCENT + player2)
                .decoration(TextDecoration.ITALIC, false);

        lore.set(1, l2);
        lore.set(2, l3);

        // Also ensure NONE of the lore lines are italicized (preserve other styling)
        List<Component> noItalics = new ArrayList<>(lore.size());
        for (Component line : lore) {
            noItalics.add((line == null ? Component.empty() : line).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(noItalics);
        item.setItemMeta(meta);
        return item;
    }


    private static String toLegacyHex() {
        String h = "#c1adfe".substring(1);
        StringBuilder sb = new StringBuilder("§x");
        for (char ch : h.toCharArray()) {
            sb.append('§').append(Character.toUpperCase(ch));
        }
        return sb.toString();
    }
}
