package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaBattleCupVictory;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgBattleCupVictory;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

@Getter
public class DotaAccountProfileCardCallback extends BaseCallbackMessage {

    private DotaProfileCard dotaProfileCard;

    public DotaAccountProfileCardCallback(CMsgDOTAProfileCard.Builder builder) {
        this.dotaProfileCard = DotaProfileCard.builder()
                .accountId(builder.getAccountId())
                .backgroundDefIndex(builder.getBackgroundDefIndex())
                .badgePoints(builder.getBadgePoints())
                .eventPoints(builder.getEventPoints())
                .eventId(builder.getEventId())
                .rankTier(builder.getRankTier())
                .leaderboardRank(builder.getLeaderboardRank())
                .isPlusSubscriber(builder.getIsPlusSubscriber())
                .plusOriginalStartDate(builder.getPlusOriginalStartDate())
                .rankTierScore(builder.getRankTierScore())
                .previousRankTier(builder.getPreviousRankTier())
                .rankTierMmrType(builder.getRankTierMmrType())
                .rankTierCore(builder.getRankTierCore())
                .rankTierCoreScore(builder.getRankTierCoreScore())
                .leaderboardRankCore(builder.getLeaderboardRankCore())
                .rankTierSupport(builder.getRankTierSupport())
                .rankTierSupportScore(builder.getRankTierSupportScore())
                .leaderboardRankSupport(builder.getLeaderboardRankSupport())
                .rankTierCorePeak(builder.getRankTierCorePeak())
                .rankTierSupportPeak(builder.getRankTierSupportPeak())
                .recentBattleCupVictory(getCupVictory(builder.getRecentBattleCupVictory()))
                .build();
    }

    private DotaBattleCupVictory getCupVictory(CMsgBattleCupVictory cupVictory) {
        return DotaBattleCupVictory.builder()
                .accountId(cupVictory.getAccountId())
                .winDate(cupVictory.getWinDate())
                .validUntil(cupVictory.getValidUntil())
                .skillLevel(cupVictory.getSkillLevel())
                .tournamentId(cupVictory.getTournamentId())
                .divisionId(cupVictory.getDivisionId())
                .teamId(cupVictory.getTeamId())
                .streak(cupVictory.getStreak())
                .trophyId(cupVictory.getTrophyId())
                .build();
    }
}
