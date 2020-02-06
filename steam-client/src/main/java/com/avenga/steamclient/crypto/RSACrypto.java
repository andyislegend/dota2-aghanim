package com.avenga.steamclient.crypto;

import com.avenga.steamclient.exception.BerDecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles encrypting and decrypting using the RSA public key encryption algorithm.
 */
public class RSACrypto {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSACrypto.class);

    private Cipher cipher;

    public RSACrypto(byte[] key) {
        Objects.requireNonNull(key, "key byte array wasn't provided");

        try {
            final List<Byte> list = new ArrayList<>();
            for (final byte b : key) {
                list.add(b);
            }
            final AsnKeyParser keyParser = new AsnKeyParser(list);
            final BigInteger[] keys = keyParser.parseRSAPublicKey();
            init(keys[0], keys[1]);
        } catch (final BerDecodeException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private void init(BigInteger mod, BigInteger exp) {
        try {
            final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(mod, exp);

            final KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey rsaKey = (RSAPublicKey) factory.generatePublic(publicKeySpec);

            cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", CryptoHelper.SEC_PROV);
            cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidKeySpecException
                | NoSuchProviderException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    public byte[] encrypt(byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (final IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return null;
    }
}
