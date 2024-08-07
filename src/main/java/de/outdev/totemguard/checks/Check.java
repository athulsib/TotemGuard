package de.outdev.totemguard.checks;

import de.outdev.totemguard.TotemGuard;
import de.outdev.totemguard.config.Settings;
import de.outdev.totemguard.discord.DiscordWebhook;
import de.outdev.totemguard.manager.AlertManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Check {

    private final ConcurrentHashMap<UUID, Integer> violations;

    private final String checkName;
    private final String checkDescription;
    private final int maxViolations;

    private final TotemGuard plugin;
    private final Settings settings;
    private final AlertManager alertManager;

    public Check(TotemGuard plugin, String checkName, String checkDescription, int maxViolations) {
        this.plugin = plugin;
        this.checkName = checkName;
        this.checkDescription = checkDescription;
        this.maxViolations = maxViolations;

        this.violations = new ConcurrentHashMap<>();

        this.settings = plugin.getConfigManager().getSettings();
        this.alertManager = plugin.getAlertManager();

        long resetInterval = settings.getPunish().getRemoveFlagsMin() * 60L * 20L; // Convert minutes to ticks (20 ticks = 1 second)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::resetAllFlagCounts, resetInterval, resetInterval);
    }

    public String getMainHandItemString(Player player) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand != null && ((ItemStack) itemInMainHand).getType() != Material.AIR) {
            ItemMeta itemMeta = itemInMainHand.getItemMeta();
            String itemName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : itemInMainHand.getType().name();
            return itemName;
        } else {
            return "Empty Hand";
        }
    }

    public final void flag(Player player, int timeDifference, int realTotem) {
        UUID uuid = player.getUniqueId();
        int totalViolations = violations.compute(uuid, (key, value) -> value == null ? 1 : value + 1);

        int ping = player.getPing();
        int tps = (int) Math.round(Bukkit.getTPS()[0]);
        int health = (int) player.getHealth();
        boolean sneaking = player.isSneaking();
        boolean blocking = player.isBlocking();
        boolean sprinting = false;
        if (player.isSwimming() || player.isSprinting() || player.isClimbing()) {
            sprinting = true;
        }

        String item = getMainHandItemString(player);
        String gamemode = String.valueOf(player.getGameMode());
        String clientBrand = player.getClientBrandName();

        Component hoverInfo = Component.text()
                .append(Component.text("Ping: ", NamedTextColor.GRAY))
                .append(Component.text(ping, NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" TPS: ", NamedTextColor.GRAY))
                .append(Component.text(tps, NamedTextColor.GOLD))
                .append(Component.text(" |", NamedTextColor.DARK_GRAY))
                .append(Component.text(" Client: ", NamedTextColor.GRAY))
                .append(Component.text(clientBrand, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Player: ", NamedTextColor.GRAY))
                .append(Component.text(player.getName(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Gamemode: ", NamedTextColor.GRAY))
                .append(Component.text(gamemode, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Health: ", NamedTextColor.GRAY))
                .append(Component.text(health, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Main Hand: ", NamedTextColor.GRAY))
                .append(Component.text(item, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Sneaking: ", NamedTextColor.GRAY))
                .append(Component.text(sneaking, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Blocking: ", NamedTextColor.GRAY))
                .append(Component.text(blocking, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Sprinting: ", NamedTextColor.GRAY))
                .append(Component.text(sprinting, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("TotemTime: ", NamedTextColor.GRAY))
                .append(Component.text(timeDifference+"ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("RealTotemTime: ", NamedTextColor.GRAY))
                .append(Component.text(realTotem+"ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Click to ", NamedTextColor.GRAY))
                .append(Component.text("teleport ", NamedTextColor.GOLD))
                .append(Component.text("to  "+player.getName()+".", NamedTextColor.GRAY))
                .build();

        Component message = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" failed ", NamedTextColor.YELLOW))
                .append(Component.text(checkName, NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text(checkDescription, NamedTextColor.GRAY))))
                .append(Component.text(" [" + totalViolations + "/" + maxViolations + "]", NamedTextColor.YELLOW))
                .append(Component.text(" (In: "+timeDifference + "ms, Ping: "+ping+")", NamedTextColor.GRAY))
                .append(Component.text(" [Info]", NamedTextColor.DARK_GRAY)
                        .hoverEvent(HoverEvent.showText(hoverInfo)))
                .build();

            alertManager.sentAlert(message);
            sendWebhookMessage(player, totalViolations);
            punishPlayer(player, totalViolations);
    }

    private void sendWebhookMessage(Player player, int totalViolations) {
        if (!settings.getWebhook().isEnabled()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("check", checkName);
            placeholders.put("violations", String.valueOf(totalViolations));
            placeholders.put("max_violations", String.valueOf(maxViolations));
            placeholders.put("client_brand", player.getClientBrandName());
            placeholders.put("ping", String.valueOf(player.getPing()));
            placeholders.put("tps", String.valueOf((int) Bukkit.getTPS()[0]));

            DiscordWebhook.sendWebhook(placeholders);
        });
    }

    private void punishPlayer(Player player, int totalViolations) {
        if (!settings.getPunish().isEnabled()) return;

        if (totalViolations >= maxViolations) {
            String punishCommand = settings.getPunish().getPunishCommand().replace("%player%", player.getName());
            violations.remove(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand);
            });
        }
    }

    private void resetAllFlagCounts() {
        violations.clear();

        alertManager.sentAlert(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text("All flag counts have been reset.", NamedTextColor.GREEN))
                .build());
    }
}
