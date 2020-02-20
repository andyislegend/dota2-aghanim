package com.avenga.steamclient.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SteamGame {
    Dota2(570),
    CounterStrikeGlobalOffensive(730),
    TeamFortress2(440),
    Artifact(583950),
    Underlords(1046930);

    private int applicationId;
}
