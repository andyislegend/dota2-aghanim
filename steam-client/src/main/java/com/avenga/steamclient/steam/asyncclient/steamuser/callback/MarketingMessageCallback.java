package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.EMarketingMessageFlags;
import com.avenga.steamclient.generated.MsgClientMarketingMessageUpdate2;
import com.avenga.steamclient.model.GlobalID;
import com.avenga.steamclient.model.steam.user.MarketingMessage;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import com.avenga.steamclient.util.stream.BinaryReader;
import com.avenga.steamclient.util.stream.MemoryStream;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * This callback is fired when the client receives a marketing message update.
 */
@Getter
public class MarketingMessageCallback extends BaseCallbackMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingMessageCallback.class);
    private static final int DEFAULT_MESSAGE_COUNT = 100;

    private Instant updateTime;
    private Collection<MarketingMessage> messages;

    public MarketingMessageCallback(MsgClientMarketingMessageUpdate2 body, byte[] payload) {
        updateTime = Instant.ofEpochMilli(body.getMarketingMessageUpdateTime() * 1000L);

        //Sometimes body.getCount() can return value greater than MAX_VALUE of the Integer.
        // Need to check this to prevent problems with heap memory.
        var messageCount = Math.min(body.getCount(), DEFAULT_MESSAGE_COUNT);

        List<MarketingMessage> marketingMessages = new ArrayList<>();

        try (var binaryReader = new BinaryReader(new MemoryStream(payload))) {
            for (int i = 0; i < messageCount; i++) {
                var totalLength = binaryReader.readInt() - 4; // total length includes the 4 byte length
                var messageData = binaryReader.readBytes(totalLength);

                marketingMessages.add(getMessage(messageData));
            }
        } catch (IOException e) {
            LOGGER.debug("Execption during reading message from protobuff: {}", e.toString());
        }

        this.messages = Collections.unmodifiableList(marketingMessages);
    }

    private MarketingMessage getMessage(byte[] messageData) {
        var marketingMessage = new MarketingMessage();
        try (var binaryReader = new BinaryReader(new ByteArrayInputStream(messageData))) {
            marketingMessage.setId(new GlobalID(binaryReader.readLong()));
            marketingMessage.setUrl(binaryReader.readNullTermString(StandardCharsets.UTF_8));
            marketingMessage.setFlags(EMarketingMessageFlags.from(binaryReader.readInt()));
        } catch (IOException e) {
            LOGGER.debug("Execption during reading message from bytes: {}", e.toString());
        }

        return marketingMessage;
    }
}
