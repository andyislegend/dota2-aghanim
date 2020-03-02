package com.avenga.steamclient.constant;

import static com.avenga.steamclient.constant.Constant.UTILITY_CLASS_INIT_ERROR;

public class ServiceMethodConstant {

    public static final String PLAYER_LAST_PLAYED_TIMES = "PlayerClient.NotifyLastPlayedTimes";
    public static final String PLAYER_FRIEND_NICKNAME_CHANGED = "PlayerClient.NotifyFriendNicknameChanged";
    public static final String PLAYER_NEW_STEAM_ANNOUNCEMENT_STATE = "PlayerClient.NotifyNewSteamAnnouncementState";
    public static final String PLAYER_COMMUNITY_PREFERENCES_CHANGED = "PlayerClient.NotifyCommunityPreferencesChanged";
    public static final String PLAYER_PER_FRIEND_PREFERENCES_CHANGED = "PlayerClient.NotifyPerFriendPreferencesChanged";
    public static final String PLAYER_PRIVACY_PRIVACY_SETTINGS_CHANGED = "PlayerClient.NotifyPrivacyPrivacySettingsChanged";

    private ServiceMethodConstant() {
        throw new IllegalStateException(UTILITY_CLASS_INIT_ERROR);
    }
}
