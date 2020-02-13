package com.avenga.steamclient.util;

import com.avenga.steamclient.protobufs.dota.BaseGCMessages;
import com.avenga.steamclient.protobufs.dota.DotaGCMessagesId;
import com.avenga.steamclient.protobufs.tf.GCSystemMessages;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class SteamEnumUtils {

    public static Optional<String> getEnumName(int messageType) {
        return getWellKnownDOTAEnumName(messageType);
    }

    public static Optional<String> getWellKnownDOTAEnumName(int messageType) {
        return Stream.<Function<Integer, ProtocolMessageEnum>>of(
                GCSystemMessages.EGCBaseClientMsg::forNumber,
                DotaGCMessagesId.EDOTAGCMsg::forNumber,
                BaseGCMessages.EGCBaseMsg::forNumber,
                GCSystemMessages.ESOMsg::forNumber)
                .map(function -> function.apply(messageType))
                .filter(Objects::nonNull)
                .map(protocolMessageEnum -> protocolMessageEnum.getValueDescriptor().getName())
                .findFirst();
    }
}
