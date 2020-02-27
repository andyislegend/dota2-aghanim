package com.avenga.steamclient.network;

import com.avenga.steamclient.crypto.CryptoHelper;
import com.avenga.steamclient.exception.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetFilterEncryptionWithHMAC implements INetFilterEncryption {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetFilterEncryptionWithHMAC.class);

    private static final int AES_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;

    private final byte[] sessionKey;
    private final byte[] hmacSecret;

    public NetFilterEncryptionWithHMAC(byte[] sessionKey) {
        if (sessionKey.length != AES_KEY_LENGTH) {
            LOGGER.debug("AES session key was not " + AES_KEY_LENGTH + " bytes!");
        }
        this.sessionKey = sessionKey;
        this.hmacSecret = new byte[IV_LENGTH];
        System.arraycopy(sessionKey, 0, hmacSecret, 0, hmacSecret.length);
    }

    @Override
    public byte[] processIncoming(byte[] data) {
        try {
            return CryptoHelper.symmetricDecryptHMACIV(data, sessionKey, hmacSecret);
        } catch (CryptoException e) {
            throw new IllegalStateException("Unable to decrypt incoming packet", e);
        }
    }

    @Override
    public byte[] processOutgoing(byte[] data) {
        try {
            return CryptoHelper.symmetricEncryptWithHMACIV(data, sessionKey, hmacSecret);
        } catch (CryptoException e) {
            throw new IllegalStateException("Unable to encrypt outgoing packet", e);
        }
    }
}
