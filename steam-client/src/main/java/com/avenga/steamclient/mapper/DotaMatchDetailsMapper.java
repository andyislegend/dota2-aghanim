package com.avenga.steamclient.mapper;

import com.avenga.steamclient.enums.EResult;
import com.avenga.steamclient.model.steam.gamecoordinator.dota.match.*;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesClient.CMsgGCMatchDetailsResponse;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesCommon;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Map proto buff {@link CMsgGCMatchDetailsResponse} class to simple POJO {@link DotaMatchDetails} class.
 */
public class DotaMatchDetailsMapper {

    public static DotaMatchDetails mapFromProto(CMsgGCMatchDetailsResponse.Builder builder) {
        var match = builder.getMatch();
        return DotaMatchDetails.builder()
                .result(EResult.from(builder.getResult()).name())
                .duration(match.getDuration())
                .startTime(match.getStartTime())
                .matchId(match.getMatchId())
                .cluster(match.getCluster())
                .firstBloodTime(match.getFirstBloodTime())
                .replaySalt(match.getReplaySalt())
                .serverIp(match.getServerIp())
                .serverPort(match.getServerPort())
                .lobbyType(match.getLobbyType())
                .humanPlayers(match.getHumanPlayers())
                .averageSkill(match.getAverageSkill())
                .gameBalance(match.getGameBalance())
                .radiantTeamId(match.getRadiantTeamId())
                .direTeamId(match.getDireTeamId())
                .leagueid(match.getLeagueid())
                .radiantTeamLogo(match.getRadiantTeamLogo())
                .direTeamLogo(match.getDireTeamLogo())
                .radiantTeamComplete(match.getRadiantTeamComplete())
                .direTeamComplete(match.getDireTeamComplete())
                .positiveVotes(match.getPositiveVotes())
                .negativeVotes(match.getNegativeVotes())
                .gameMode(match.getGameMode().getNumber())
                .matchSeqNum(match.getMatchSeqNum())
                .replayState(match.getReplayState().getNumber())
                .radiantGuildId(match.getRadiantGuildId())
                .direGuildId(match.getDireGuildId())
                .seriesId(match.getSeriesId())
                .seriesType(match.getSeriesType())
                .engine(match.getEngine())
                .matchFlags(match.getMatchFlags())
                .privateMetadataKey(match.getPrivateMetadataKey())
                .radiantTeamScore(match.getRadiantTeamScore())
                .direTeamScore(match.getDireTeamScore())
                .matchOutcome(match.getMatchFlags())
                .tournamentId(match.getTournamentId())
                .tournamentRound(match.getTournamentRound())
                .preGameDuration(match.getPreGameDuration())
                .picksBans(getPickBans(match))
                .players(getPlayers(match))
                .build();
    }

    private static List<DotaPicksBans> getPickBans(DotaGCMessagesCommon.CMsgDOTAMatch match) {
        return match.getPicksBansList().stream()
                .map(pickBan -> DotaPicksBans.builder()
                        .heroId(pickBan.getHeroId())
                        .isPick(pickBan.getIsPick())
                        .team(pickBan.getTeam())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DotaMatchDetailsPlayer> getPlayers(DotaGCMessagesCommon.CMsgDOTAMatch match) {
        return match.getPlayersList().stream()
                .map(player -> DotaMatchDetailsPlayer.builder()
                        .accountId(player.getAccountId())
                        .playerSlot(player.getPlayerSlot())
                        .heroId(player.getHeroId())
                        .item0(player.getItem0())
                        .item1(player.getItem1())
                        .item2(player.getItem2())
                        .item3(player.getItem3())
                        .item4(player.getItem4())
                        .item5(player.getItem5())
                        .item6(player.getItem6())
                        .item7(player.getItem7())
                        .item8(player.getItem8())
                        .item9(player.getItem9())
                        .expectedTeamContribution(player.getExpectedTeamContribution())
                        .scaledMetric(player.getScaledMetric())
                        .previousRank(player.getPreviousRank())
                        .rankChange(player.getRankChange())
                        .mmrType(player.getMmrType())
                        .rankTierUpdated(player.getRankTierUpdated())
                        .kills(player.getKills())
                        .deaths(player.getDeaths())
                        .assists(player.getAssists())
                        .leaverStatus(player.getLeaverStatus())
                        .gold(player.getGold())
                        .lastHits(player.getLastHits())
                        .denies(player.getDenies())
                        .goldPerMin(player.getGoldPerMin())
                        .xPPerMin(player.getXPPerMin())
                        .goldSpent(player.getGoldSpent())
                        .heroDamage(player.getHeroDamage())
                        .towerDamage(player.getTowerDamage())
                        .heroHealing(player.getHeroHealing())
                        .level(player.getLevel())
                        .timeLastSeen(player.getTimeLastSeen())
                        .playerName(player.getPlayerName())
                        .supportAbilityValue(player.getSupportAbilityValue())
                        .feedingDetected(player.getFeedingDetected())
                        .searchRank(player.getSearchRank())
                        .searchRankUncertainty(player.getSearchRankUncertainty())
                        .rankUncertaintyChange(player.getRankUncertaintyChange())
                        .heroPlayCount(player.getHeroPlayCount())
                        .partyId(player.getPartyId())
                        .scaledHeroDamage(player.getScaledHeroDamage())
                        .scaledTowerDamage(player.getScaledTowerDamage())
                        .scaledHeroHealing(player.getScaledHeroHealing())
                        .scaledKills(player.getScaledKills())
                        .scaledDeaths(player.getScaledDeaths())
                        .scaledAssists(player.getScaledAssists())
                        .claimedFarmGold(player.getClaimedFarmGold())
                        .supportGold(player.getSupportGold())
                        .claimedDenies(player.getClaimedDenies())
                        .claimedMisses(player.getClaimedMisses())
                        .misses(player.getMisses())
                        .proName(player.getProName())
                        .realName(player.getRealName())
                        .activePlusSubscription(player.getActivePlusSubscription())
                        .netWorth(player.getNetWorth())
                        .botDifficulty(player.getBotDifficulty())
                        .heroPickOrder(player.getHeroPickOrder())
                        .heroWasRandomed(player.getHeroWasRandomed())
                        .heroWasDotaPlusSuggestion(player.getHeroWasDotaPlusSuggestion())
                        .secondsDead(player.getSecondsDead())
                        .goldLostToDeath(player.getGoldLostToDeath())
                        .laneSelectionFlags(player.getLaneSelectionFlags())
                        .abilityUpgrades(getUpgrades(player))
                        .additionalUnitInventories(getUnitInventories(player))
                        .permanentBuffs(getPermanentBuffs(player))
                        .heroDamageReceived(getDamageReceivedList(player))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DotaPlayerAbilityUpgrade> getUpgrades(DotaGCMessagesCommon.CMsgDOTAMatch.Player player) {
        return player.getAbilityUpgradesList().stream()
                .map(upgrade -> DotaPlayerAbilityUpgrade.builder()
                        .ability(upgrade.getAbility())
                        .time(upgrade.getTime())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DotaAdditionalUnitInventory> getUnitInventories(DotaGCMessagesCommon.CMsgDOTAMatch.Player player) {
        return player.getAdditionalUnitsInventoryList().stream()
                .map(unitInventory -> DotaAdditionalUnitInventory.builder()
                        .unitName(unitInventory.getUnitName())
                        .items(unitInventory.getItemsList())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DotaPlayerPermanentBuff> getPermanentBuffs(DotaGCMessagesCommon.CMsgDOTAMatch.Player player) {
        return player.getPermanentBuffsList().stream()
                .map(buff -> DotaPlayerPermanentBuff.builder()
                        .permanentBuff(buff.getPermanentBuff())
                        .stackCount(buff.getStackCount())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DotaHeroDamageReceived> getDamageReceivedList(DotaGCMessagesCommon.CMsgDOTAMatch.Player player) {
        return player.getHeroDamageReceivedList().stream()
                .map(damageReceived -> DotaHeroDamageReceived.builder()
                        .damageType(damageReceived.getDamageType().getNumber())
                        .postReduction(damageReceived.getPostReduction())
                        .preReduction(damageReceived.getPreReduction())
                        .build())
                .collect(Collectors.toList());
    }
}
