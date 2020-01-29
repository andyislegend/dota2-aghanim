package com.avenga.steamclient.steam.handler;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesBase.CMsgMulti;
import com.avenga.steamclient.steam.CMClient;
import com.avenga.steamclient.util.stream.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static com.avenga.steamclient.steam.CMClient.getPacketMsg;

public class MultiClientPacketHandler implements ClientPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiClientPacketHandler.class);

    @Override
    public void handle(PacketMessage packetMessage, CMClient cmClient) {
        if (!packetMessage.isProto()) {
            LOGGER.debug("HandleMulti got non-proto MsgMulti!!");
            return;
        }

        ClientMessageProtobuf<CMsgMulti.Builder> multiMessage = new ClientMessageProtobuf<>(CMsgMulti.class, packetMessage);

        byte[] payload = multiMessage.getBody().getMessageBody().toByteArray();

        if (multiMessage.getBody().getSizeUnzipped() > 0) {
            try {
                GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(payload));
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                int size = 0;
                byte[] buffer = new byte[1024];
                while (size >= 0) {
                    size = gzipInputStream.read(buffer, 0, buffer.length);
                    if (size > 0) {
                        outputStream.write(buffer, 0, size);
                    }
                }
                payload = outputStream.toByteArray();
            } catch (IOException e) {
                LOGGER.debug("HandleMulti encountered an exception when decompressing.", e);
                return;
            }
        }

        List<PacketMessage> packetMessages = new ArrayList<>();
        try (BinaryReader binaryReader = new BinaryReader(new ByteArrayInputStream(payload))) {
            while (binaryReader.available() > 0) {
                int subSize = binaryReader.readInt();
                byte[] subData = binaryReader.readBytes(subSize);

                PacketMessage subPacketMessage = getPacketMsg(subData);
                packetMessages.add(subPacketMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (PacketMessage message : packetMessages) {
            LOGGER.debug(String.format("<- Part of Multi - EMsg: %s (%d) (Proto: %s)", message.getMessageType(), message.getMessageType().code(), message.isProto()));
            if (!cmClient.onClientMsgReceived(message)) {
                break;
            }
        }
    }
}
