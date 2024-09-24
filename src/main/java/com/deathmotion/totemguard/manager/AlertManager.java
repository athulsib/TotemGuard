/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.models.CheckDetails;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.database.DatabaseService;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {

    @Getter
    private final ConcurrentHashMap<UUID, Player> enabledAlerts;

    private final TotemGuard plugin;
    private final DatabaseService databaseService;

    public AlertManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();
        this.enabledAlerts = new ConcurrentHashMap<>();
    }

    public void sendAlert(TotemPlayer totemPlayer, CheckDetails alert) {
        enabledAlerts.values().forEach(player -> player.sendMessage(alert.getAlert()));
        databaseService.saveAlert(totemPlayer, alert);
    }

    public void sendAlert(Component message) {
        enabledAlerts.values().forEach(player -> player.sendMessage(message));
    }

    public void toggleAlerts(Player player) {
        UUID playerId = player.getUniqueId();
        if (enabledAlerts.containsKey(playerId)) {
            enabledAlerts.remove(playerId);
            sendAlertStatusMessage(player, "Alerts disabled", NamedTextColor.RED);
        } else {
            enabledAlerts.put(playerId, player);
            sendAlertStatusMessage(player, "Alerts enabled", NamedTextColor.GREEN);
        }
    }

    public void enableAlerts(Player player) {
        UUID playerId = player.getUniqueId();
        if (!enabledAlerts.containsKey(playerId)) {
            enabledAlerts.put(playerId, player);
            sendAlertStatusMessage(player, "Alerts enabled!", NamedTextColor.GREEN);
        }
    }

    public void removePlayer(UUID playerId) {
        enabledAlerts.remove(playerId);
    }

    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.containsKey(player.getUniqueId());
    }

    private void sendAlertStatusMessage(Player player, String message, NamedTextColor color) {
        final Settings settings = plugin.getConfigManager().getSettings();

        player.sendMessage(Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(settings.getPrefix()))
                .append(Component.text(message, color))
                .build());
    }
}