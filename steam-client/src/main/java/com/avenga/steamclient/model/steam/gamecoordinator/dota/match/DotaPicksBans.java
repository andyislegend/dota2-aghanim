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

    /**
     *  0 - Radiant side, 1 - Dire side
     */
    private int team;
}
