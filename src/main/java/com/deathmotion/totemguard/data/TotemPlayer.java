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

package com.deathmotion.totemguard.data;

import com.deathmotion.totemguard.util.MathUtil;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Getter
public class TotemPlayer {
    @Setter
    private UUID uuid;
    @Setter
    private String username;
    @Setter
    private String clientBrandName;
    @Setter
    private ClientVersion clientVersion;
    @Setter
    private boolean bedrockPlayer;
    @Setter
    private double latestStandardDeviation;

    @Getter
    private final ConcurrentLinkedDeque<Long> intervals = new ConcurrentLinkedDeque<>();

    public void addInterval(long interval) {
        if (intervals.size() >= 50) {
            intervals.poll();
        }

        intervals.add(interval);

        latestStandardDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(intervals));
    }

    public List<Long> getLatestIntervals(int amount) {
        return intervals.stream()
                .skip(Math.max(0, intervals.size() - amount))
                .toList();
    }
}
