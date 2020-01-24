package com.avenga.steamclient.steam;

import com.avenga.steamclient.base.ClientPacketMessage;
import com.avenga.steamclient.base.ClientProtobufPacketMessage;
import com.avenga.steamclient.base.DefaultPacketMessage;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.util.MessageUtil;
import com.avenga.steamclient.util.stream.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CMClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMClient.class);

    public static PacketMessage getPacketMsg(byte[] data) {
        if (data.length < 4) {
            LOGGER.debug("PacketMsg too small to contain a message, was only {0} bytes. Message: 0x{1}");
            return null;
        }

        BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data));

        int rawEMsg = 0;
        try {
            rawEMsg = reader.readInt();
        } catch (IOException e) {
            LOGGER.debug("Exception while getting EMsg code", e);
        }
        EMsg eMsg = MessageUtil.getMessage(rawEMsg);

        switch (eMsg) {
            case ChannelEncryptRequest:
            case ChannelEncryptResponse:
            case ChannelEncryptResult:
                try {
                    return new DefaultPacketMessage(eMsg, data);
                } catch (IOException e) {
                    LOGGER.debug("Exception deserializing emsg " + eMsg + " (" + MessageUtil.isProtoBuf(rawEMsg) + ").", e);
                }
        }

        try {
            if (MessageUtil.isProtoBuf(rawEMsg)) {
                return new ClientProtobufPacketMessage(eMsg, data);
            } else {
                return new ClientPacketMessage(eMsg, data);
            }
        } catch (IOException e) {
            LOGGER.debug("Exception deserializing emsg " + eMsg + " (" + MessageUtil.isProtoBuf(rawEMsg) + ").", e);
            return null;
        }
    }
}
