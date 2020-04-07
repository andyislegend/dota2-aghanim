package com.avenga.steamclient.util;

import com.avenga.steamclient.enums.SteamGame;
import com.avenga.steamclient.protobufs.dota.BaseGCMessages;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesId;
import com.avenga.steamclient.protobufs.dota.GcSystemMessages;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class SteamEnumUtils {

    private static final Map<Integer, Function<Integer, Optional<String>>> ENUM_NAME_PER_APPLICATION = Map.of(
            SteamGame.Dota2.getApplicationId(), SteamEnumUtils::getWellKnownDOTAEnumName
    );

    public static Optional<String> getEnumName(int messageType, int applicationId) {
        var enumFunction = ENUM_NAME_PER_APPLICATION.get(applicationId);

        return Objects.nonNull(enumFunction) ? enumFunction.apply(messageType) : Optional.empty();
    }

    public static Optional<String> getWellKnownDOTAEnumName(int messageType) {
        return Stream.<Function<Integer, ProtocolMessageEnum>>of(
                GcSystemMessages.EGCBaseClientMsg::forNumber,
                DotaGCMessagesId.EDOTAGCMsg::forNumber,
                BaseGCMessages.EGCBaseMsg::forNumber,
                GcSystemMessages.ESOMsg::forNumber)
                .map(function -> function.apply(messageType))
                .filter(Objects::nonNull)
                .map(protocolMessageEnum -> protocolMessageEnum.getValueDescriptor().getName())
                .findFirst();
    }
}
