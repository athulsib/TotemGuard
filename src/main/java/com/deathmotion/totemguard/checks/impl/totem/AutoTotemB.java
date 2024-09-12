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

package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.TotemEventListener;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class AutoTotemB extends Check implements TotemEventListener {

    private final TotemGuard plugin;

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Impossible consistency", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(15);
        if (intervals.size() < 4) return;

        // Calculate standard deviation, mean, skewness, and outliers
        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        double mean = MathUtil.getMean(intervals);
        double skewness = MathUtil.getSkewness(intervals);
        List<Double> lowOutliers = MathUtil.getOutliers(intervals).getX();

        plugin.debug("===================");
        plugin.debug("Player: " + player.getName());
        plugin.debug("Standard Deviation: " + standardDeviation);
        plugin.debug("Mean: " + mean);
        plugin.debug("Skewness: " + skewness);
        plugin.debug("Low Outliers: " + lowOutliers);
    }

    private Component createComponent(double sd, double mean, double skewness) {
        return Component.text()
                .append(Component.text("SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(sd + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Mean" + ": ", NamedTextColor.GRAY))
                .append(Component.text(mean + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Skewness" + ": ", NamedTextColor.GRAY))
                .append(Component.text(skewness, NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void resetData() {

    }

    @Override
    public void resetData(UUID uuid) {

    }
}
