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

    private Instant updateTime;
    private Collection<MarketingMessage> messages;

    public MarketingMessageCallback(MsgClientMarketingMessageUpdate2 body, byte[] payload) {
        updateTime = Instant.ofEpochMilli(body.getMarketingMessageUpdateTime() * 1000L);

        List<MarketingMessage> marketingMessages = new ArrayList<>(body.getCount());

        try (var binaryReader = new BinaryReader(new MemoryStream(payload))) {
            for (int i = 0; i < body.getCount(); i++) {
                var totalLength = binaryReader.readInt() - 4; // total length includes the 4 byte length
                var messageData = binaryReader.readBytes(totalLength);

                marketingMessages.add(getMessage(messageData));
            }
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
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
            LOGGER.debug(e.getMessage(), e);
        }

        return marketingMessage;
    }
}
