package com.avenga.steamclient.model.steam.gamecoordinator.dota.match;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaHeroDamageReceived {
    private int preReduction;
    private int postReduction;
    private int damageType;
}
