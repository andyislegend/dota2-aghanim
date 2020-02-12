package com.avenga.steamclient.util;

import com.avenga.steamclient.protobufs.dota.BaseGCMessages;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesId;
import com.avenga.steamclient.protobufs.tf.GCSystemMessages;

import java.util.Objects;

public class SteamEnumUtils {

    public static String getEnumName(int messageType) {
        var enumName = getWellKnownDOTAEnumName(messageType);

        if (!StringUtils.isNullOrEmpty(enumName)) {
            return enumName;
        }

        return "";
    }

    public static String getWellKnownDOTAEnumName(int messageType) {
        if (Objects.nonNull(GCSystemMessages.EGCBaseClientMsg.forNumber(messageType))) {
            return GCSystemMessages.EGCBaseClientMsg.forNumber(messageType).name();
        } else if (Objects.nonNull(DotaGCMessagesId.EDOTAGCMsg.forNumber(messageType))) {
            return DotaGCMessagesId.EDOTAGCMsg.forNumber(messageType).name();
        } else if (Objects.nonNull(BaseGCMessages.EGCBaseMsg.forNumber(messageType))) {
            return BaseGCMessages.EGCBaseMsg.forNumber(messageType).name();
        } else if (Objects.nonNull(GCSystemMessages.ESOMsg.forNumber(messageType))) {
            return GCSystemMessages.ESOMsg.forNumber(messageType).name();
        }

        return "";
    }
}
