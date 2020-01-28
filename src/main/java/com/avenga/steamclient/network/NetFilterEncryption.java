package com.avenga.steamclient.network;

import com.avenga.steamclient.crypto.CryptoHelper;
import com.avenga.steamclient.exception.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetFilterEncryption implements INetFilterEncryption {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetFilterEncryption.class);

    private static final int AES_KEY_LENGTH = 32;

    private final byte[] sessionKey;

    public NetFilterEncryption(byte[] sessionKey) {
        if (sessionKey.length != AES_KEY_LENGTH) {
            LOGGER.debug(String.format("AES session key was not %d bytes!", AES_KEY_LENGTH));
        }
        this.sessionKey = sessionKey;
    }

    @Override
    public byte[] processIncoming(byte[] data) {
        try {
            return CryptoHelper.symmetricDecrypt(data, sessionKey);
        } catch (CryptoException e) {
            throw new IllegalStateException("Unable to decrypt incoming packet", e);
        }
    }

    @Override
    public byte[] processOutgoing(byte[] data) {
        try {
            return CryptoHelper.symmetricEncrypt(data, sessionKey);
        } catch (CryptoException e) {
            throw new IllegalStateException("Unable to encrypt outgoing packet", e);
        }
    }
}
