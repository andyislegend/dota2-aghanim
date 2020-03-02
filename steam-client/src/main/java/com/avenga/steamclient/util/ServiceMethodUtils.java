package com.avenga.steamclient.util;

import com.avenga.steamclient.protobufs.steamclient.SteammessagesPlayerSteamclient.*;

import java.util.Map;

import static com.avenga.steamclient.constant.ServiceMethodConstant.*;

public class ServiceMethodUtils {

    private static final String METHOD_SPLITTER = "#";

    public static Class<?> getServiceMethodClass(String jobTargetName) {
        var interfaceMethod = jobTargetName.split(METHOD_SPLITTER)[0];

        return getPlayerMethodMapping().get(interfaceMethod);
    }

    private static Map<String, Class<?>> getPlayerMethodMapping() {
        return Map.of(
                PLAYER_LAST_PLAYED_TIMES, CPlayer_GetLastPlayedTimes_Response.class,
                PLAYER_FRIEND_NICKNAME_CHANGED, CPlayer_FriendNicknameChanged_Notification.class,
                PLAYER_NEW_STEAM_ANNOUNCEMENT_STATE, CPlayer_NewSteamAnnouncementState_Notification.class,
                PLAYER_COMMUNITY_PREFERENCES_CHANGED, CPlayer_CommunityPreferencesChanged_Notification.class,
                PLAYER_PER_FRIEND_PREFERENCES_CHANGED, CPlayer_PerFriendPreferencesChanged_Notification.class,
                PLAYER_PRIVACY_PRIVACY_SETTINGS_CHANGED, CPlayer_PrivacySettingsChanged_Notification.class
        );
    }
}
