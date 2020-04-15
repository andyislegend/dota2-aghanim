package com.avenga.steamclient.mapper;

import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaBattleCupVictory;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.account.DotaProfileCard;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon.CMsgDOTAProfileCard;

/**
 * Map proto buff {@link CMsgDOTAProfileCard} class to simple POJO {@link DotaProfileCard} class.
 */
public class DotaAccountProfileCardMapper {

    public static DotaProfileCard mapFromProto(CMsgDOTAProfileCard.Builder builder) {
        return DotaProfileCard.builder()
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
                .leaderboardRankCore(builder.getLeaderboardRankCore())
                .rankTierPeak(builder.getRankTierPeak())
                .recentBattleCupVictory(getCupVictory(builder.getRecentBattleCupVictory()))
                .build();
    }

    private static DotaBattleCupVictory getCupVictory(DotaGCMessagesCommon.CMsgBattleCupVictory cupVictory) {
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
