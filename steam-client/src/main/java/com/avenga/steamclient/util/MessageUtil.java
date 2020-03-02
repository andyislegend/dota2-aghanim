package com.avenga.steamclient.util;

import com.avenga.steamclient.base.ClientMessageProtobuf;
import com.avenga.steamclient.base.PacketMessage;
import com.avenga.steamclient.base.SteamSerializableHeader;
import com.avenga.steamclient.enums.EMsg;
import com.avenga.steamclient.generated.ExtendedClientMsgHdr;
import com.avenga.steamclient.generated.MsgHdr;
import com.avenga.steamclient.generated.MsgHdrProtoBuf;
import com.avenga.steamclient.util.stream.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.avenga.steamclient.enums.EMsg.*;

public class MessageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);

    private static final int PROTO_MASK = 0x80000000;
    private static final long MAX_UINT_MASK = 0xffffffffL;
    private static final int EMSG_MASK = ~PROTO_MASK;
    private static final List<Integer> MESSAGE_HEADER_CODES = List.of(ChannelEncryptRequest.code(), ChannelEncryptResponse.code(),
            ChannelEncryptResult.code());

    /**
     * Strips off the protobuf message flag and returns an EMsg.
     *
     * @param msg The message number.
     * @return The underlying EMsg.
     */
    public static EMsg getMessage(int msg) {
        return EMsg.from(msg & EMSG_MASK);
    }

    /**
     * Strips off the protobuf message flag and returns an EMsg.
     *
     * @param msg The message number.
     * @return The underlying EMsg.
     */
    public static int getGCMsg(int msg) {
        return msg & EMSG_MASK;
    }

    /**
     * Crafts an EMsg, flagging it if required.
     *
     * @param msg      The EMsg to flag.
     * @param protobuf if set to true, the message is protobuf flagged.
     * @return A crafted EMsg, flagged if requested.
     */
    public static int makeMessage(int msg, boolean protobuf) {
        if (protobuf) {
            return msg | PROTO_MASK;
        }

        return msg;
    }

    /**
     * Crafts an EMsg, flagging it if required.
     *
     * @param msg      The EMsg to flag.
     * @param protobuf if set to <b>true</b>, the message is protobuf flagged.
     * @return A crafted EMsg, flagged if requested
     */
    public static int makeGCMsg(int msg, boolean protobuf) {
        if (protobuf) {
            return msg | PROTO_MASK;
        }
        return msg;
    }

    /**
     * Determines whether message is protobuf flagged.
     *
     * @param msg The message.
     * @return <b>true</b> if this message is protobuf flagged; otherwise, <b>false</b>.
     */
    public static boolean isProtoBuf(int msg) {
        return (msg & MAX_UINT_MASK & PROTO_MASK) > 0;
    }

    /**
     * Reads packet message header received from Steam Network.
     *
     * @param packetMessage Stean Network packet message.
     * @return deserialized header of the packet message.
     */
    public static SteamSerializableHeader readHeader(PacketMessage packetMessage) {
        SteamSerializableHeader header;

        if (packetMessage.isProto()) {
            header = new MsgHdrProtoBuf();
        } else if (MESSAGE_HEADER_CODES.contains(packetMessage.getMessageType().code())) {
            header = new MsgHdr();
        } else {
            header = new ExtendedClientMsgHdr();
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(packetMessage.getData())) {
            header.deserialize(stream);
        } catch (IOException e) {
            LOGGER.debug("Fail to deserialize rawEMsg {} header: {}", packetMessage.getMessageType().code(), e.getMessage());
        }
        return header;
    }

    /**
     * Provide body message extreacted from ServiceMethod.
     *
     * @param packetMessage Stean Network packet message.
     * @return ClientMessageProtobuf of the body message.
     */
    public static Optional<ClientMessageProtobuf> readServiceMethodBody(PacketMessage packetMessage) {
        var header = readHeader(packetMessage);
        Optional<ClientMessageProtobuf> messageProtobuf = Optional.empty();

        if (packetMessage.isProto()) {
            var headerBody = ((MsgHdrProtoBuf) header).getProto().build();

            if (ServiceMethod.code() == packetMessage.getMessageType().code()) {
                var protoClass = ServiceMethodUtils.getServiceMethodClass(headerBody.getTargetJobName());
                if (Objects.nonNull(protoClass) && packetMessage.isProto()) {
                    messageProtobuf = Optional.of(new ClientMessageProtobuf(protoClass, packetMessage));
                }
            }
        }

        return messageProtobuf;
    }

    /**
     * Gets raw EMsg ID from Steam network data.
     *
     * @param data Steam network data
     * @return raw EMsg ID.
     */
    public static int getRawEMsg(byte[] data) {
        try (BinaryReader reader = new BinaryReader(new ByteArrayInputStream(data))) {
            return reader.readInt();
        } catch (IOException e) {
            LOGGER.debug("Exception while getting EMsg code", e);
            return 0;
        }
    }
}
