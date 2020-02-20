package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EMarketingMessageFlags;
import com.avenga.steamclient.generated.MsgClientMarketingMessageUpdate2;
import com.avenga.steamclient.model.GlobalID;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import com.avenga.steamclient.util.stream.BinaryReader;
import com.avenga.steamclient.util.stream.MemoryStream;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This callback is fired when the client receives a marketing message update.
 */
@Getter
public class MarketingMessageCallback extends BaseCallbackMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingMessageCallback.class);

    private Date updateTime;
    private Collection<Message> messages;

    public MarketingMessageCallback(MsgClientMarketingMessageUpdate2 body, byte[] payload) {
        updateTime = new Date(body.getMarketingMessageUpdateTime() * 1000L);

        List<Message> msgList = new ArrayList<>();

        try (BinaryReader binaryReader = new BinaryReader(new MemoryStream(payload))) {
            for (int i = 0; i < body.getCount(); i++) {
                int totalLength = binaryReader.readInt() - 4; // total length includes the 4 byte length
                byte[] messageData = binaryReader.readBytes(totalLength);

                msgList.add(new Message(messageData));
            }
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        messages = Collections.unmodifiableList(msgList);
    }

    /**
     * Represents a single marketing message.
     */
    @Getter
    public static class Message {
        private GlobalID id;
        private String url;
        private EnumSet<EMarketingMessageFlags> flags;

        Message(byte[] data) {
            try (BinaryReader br = new BinaryReader(new ByteArrayInputStream(data))) {
                id = new GlobalID(br.readLong());
                url = br.readNullTermString(StandardCharsets.UTF_8);
                flags = EMarketingMessageFlags.from(br.readInt());
            } catch (IOException e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
    }
}
