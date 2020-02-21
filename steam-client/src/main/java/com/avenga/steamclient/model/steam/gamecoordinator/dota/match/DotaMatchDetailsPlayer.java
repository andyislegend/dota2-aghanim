package com.avenga.steamclient.model.steam.gamecoordinator.dota.match;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaMatchDetailsPlayer {
    private int accountId;
    private int playerSlot;
    private int heroId;
    private int item0;
    private int item1;
    private int item2;
    private int item3;
    private int item4;
    private int item5;
    private int item6;
    private int item7;
    private int item8;
    private int item9;
    private float expectedTeamContribution;
    private float scaledMetric;
    private int previousRank;
    private int rankChange;
    private int mmrType;
    private boolean rankTierUpdated;
    private int kills;
    private int deaths;
    private int assists;
    private int leaverStatus;
    private int gold;
    private int lastHits;
    private int denies;
    private int goldPerMin;
    private int xPPerMin;
    private int goldSpent;
    private int heroDamage;
    private int towerDamage;
    private int heroHealing;
    private int level;
    private int timeLastSeen;
    private String playerName;
    private int supportAbilityValue;
    private boolean feedingDetected;
    private int searchRank;
    private int searchRankUncertainty;
    private int rankUncertaintyChange;
    private int heroPlayCount;
    private long partyId;
    private int scaledHeroDamage;
    private int scaledTowerDamage;
    private int scaledHeroHealing;
    private float scaledKills;
    private float scaledDeaths;
    private float scaledAssists;
    private int claimedFarmGold;
    private int supportGold;
    private int claimedDenies;
    private int claimedMisses;
    private int misses;
    private String proName;
    private String realName;
    private boolean activePlusSubscription;
    private int netWorth;
    private int botDifficulty;
    private int heroPickOrder;
    private boolean heroWasRandomed;
    private boolean heroWasDotaPlusSuggestion;
    private int secondsDead;
    private int goldLostToDeath;
    private int laneSelectionFlags;
    private List<DotaPlayerAbilityUpgrade> abilityUpgrades;
    private List<DotaAdditionalUnitInventory> additionalUnitInventories;
    private List<DotaPlayerPermanentBuff> permanentBuffs;
    private List<DotaHeroDamageReceived> heroDamageReceived;
}
