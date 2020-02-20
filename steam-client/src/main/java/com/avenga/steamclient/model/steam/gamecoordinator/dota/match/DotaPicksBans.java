package com.avenga.steamclient.model.steam.gamecoordinator.dota.match;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaPicksBans {
    private boolean isPick;
    private int heroId;
    private int team; // 0 - Radiant, 1 - Dire
}
