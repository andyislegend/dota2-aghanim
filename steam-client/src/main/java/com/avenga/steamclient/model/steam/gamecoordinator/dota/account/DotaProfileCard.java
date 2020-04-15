package com.avenga.steamclient.model.steam.gamecoordinator.dota.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaProfileCard {
    private int accountId;
    private int backgroundDefIndex;
    private int badgePoints;
    private int eventPoints;
    private int eventId;
    private int rankTier;
    private int leaderboardRank;
    private boolean isPlusSubscriber;
    private int plusOriginalStartDate;
    private int rankTierScore;
    private int previousRankTier;
    private int rankTierMmrType;
    private int leaderboardRankCore;
    private int leaderboardRankSupport;
    private int rankTierPeak;
    private DotaBattleCupVictory recentBattleCupVictory;
}
