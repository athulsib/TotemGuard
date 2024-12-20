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

package com.deathmotion.totemguard.util.messages;

import com.deathmotion.totemguard.database.entities.Check;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.models.SafetyStatus;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileCreator {

    public DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    public ZoneId zoneId = ZoneId.systemDefault();

    public Component createProfileComponent(String username, List<Alert> alerts, List<Punishment> punishments, long loadTime, SafetyStatus safetyStatus, Pair<TextColor, TextColor> colorScheme) {
        // Group alerts by check name and count them
        Map<Check, Long> checkCounts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getCheckName, Collectors.counting()));

        // Sort the map entries by count in descending order
        List<Map.Entry<Check, Long>> sortedCheckCounts = checkCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        // Start building the component using a builder
        TextComponent.Builder componentBuilder = Component.text()
                .append(Component.text("TotemGuard Profile", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Player: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(username, colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Safety Status: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(safetyStatus.getName(), safetyStatus.getColor()))
                .append(Component.newline())
                .append(Component.text("Total Logs: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(alerts.size(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Total Punishments: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(punishments.size(), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Load Time: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(loadTime + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("> Alert Summary <", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline());

        if (sortedCheckCounts.isEmpty()) {
            componentBuilder.append(Component.text(" No logs found.", colorScheme.getY(), TextDecoration.ITALIC));
        } else {
            sortedCheckCounts.forEach(entry -> {
                Check checkName = entry.getKey();
                Long count = entry.getValue();
                componentBuilder.append(Component.text("- ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(checkName + ": ", colorScheme.getY(), TextDecoration.BOLD))
                        .append(Component.text(count + "x", colorScheme.getX()))
                        .append(Component.newline());
            });
        }

        componentBuilder.append(Component.newline())
                .append(Component.text("> Punishments <", colorScheme.getX(), TextDecoration.BOLD))
                .append(Component.newline());

        if (punishments.isEmpty()) {
            componentBuilder.append(Component.text(" No punishments found.", colorScheme.getY(), TextDecoration.ITALIC));
        }
        if (punishments.size() > 3) {
            componentBuilder.append(Component.text("Showing the last 3 punishments:", colorScheme.getY(), TextDecoration.ITALIC))
                    .append(Component.newline());
        }

        // Sort the punishments by whenCreated in descending order (newest first)
        List<Punishment> sortedPunishments = punishments.stream()
                .sorted(Comparator.comparing(Punishment::getWhenCreated).reversed())
                .toList();

        List<Punishment> recentPunishments = sortedPunishments.stream()
                .limit(3) // Take the first 3 elements (the newest ones)
                .toList();

        recentPunishments.forEach(punishment -> {
            ZonedDateTime punishmentTime = ZonedDateTime.ofInstant(punishment.getWhenCreated(), zoneId);
            String formattedDate = punishmentTime.format(shortFormatter);
            String relativeTime = getRelativeTime(punishment.getWhenCreated());

            componentBuilder.append(Component.text("- ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Punished for ", colorScheme.getY()))
                    .append(Component.text(String.valueOf(punishment.getCheckName()), colorScheme.getX(), TextDecoration.BOLD))
                    .append(Component.text(" on ", colorScheme.getY()))
                    .append(Component.text(formattedDate, colorScheme.getX())
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Occurred " + relativeTime, colorScheme.getY())
                            )))
                    .append(Component.newline());
        });

        if (punishments.size() > 5) {
            componentBuilder.append(Component.text("... and more not displayed", colorScheme.getY(), TextDecoration.ITALIC))
                    .append(Component.newline());
        }


        return componentBuilder.build();
    }

    public String getRelativeTime(Instant past) {
        Duration duration = Duration.between(past, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "just now";
        } else if (seconds < 3600) {
            return seconds / 60 + " minutes ago";
        } else if (seconds < 86400) {
            return seconds / 3600 + " hours ago";
        } else {
            return seconds / 86400 + " days ago";
        }
    }
}
