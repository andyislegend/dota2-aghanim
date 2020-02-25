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
    private int rankTierCore;
    private int rankTierCoreScore;
    private int leaderboardRankCore;
    private int rankTierSupport;
    private int rankTierSupportScore;
    private int leaderboardRankSupport;
    private int rankTierCorePeak;
    private int rankTierSupportPeak;
    private DotaBattleCupVictory recentBattleCupVictory;
}
