package com.avenga.steamclient.model.steam.gamecoordinator.dota.match;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DotaMatchDetails {
    private int duration;
    private int startTime;
    private long matchId;
    private int cluster;
    private int firstBloodTime;
    private int replaySalt;
    private int serverIp;
    private int serverPort;
    private int lobbyType;
    private int humanPlayers;
    private int averageSkill;
    private float gameBalance;
    private int radiantTeamId;
    private int direTeamId;
    private int leagueid;
    private long radiantTeamLogo;
    private long direTeamLogo;
    private int radiantTeamComplete;
    private int direTeamComplete;
    private int positiveVotes;
    private int negativeVotes;
    private int gameMode;
    private long matchSeqNum;
    private int replayState;
    private int radiantGuildId;
    private int direGuildId;
    private int seriesId;
    private int seriesType;
    private int engine;
    private int matchFlags;
    private int privateMetadataKey;
    private int radiantTeamScore;
    private int direTeamScore;
    private int matchOutcome;
    private int tournamentId;
    private int tournamentRound;
    private int preGameDuration;
    private List<DotaPicksBans> picksBans;
    private List<DotaMatchDetailsPlayer> players;
}
