package com.avenga.steamclient.model.steam.gamecoordinator.dota.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaBattleCupVictory {
    private int accountId;
    private int winDate;
    private int validUntil;
    private int skillLevel;
    private int tournamentId;
    private int divisionId;
    private int teamId;
    private int streak;
    private int trophyId;
}
