package com.avenga.steamclient.crypto;

import com.avenga.steamclient.enums.EOSType;
import com.avenga.steamclient.exception.CryptoException;
import com.avenga.steamclient.model.Passable;
import com.avenga.steamclient.util.Utils;
import com.avenga.steamclient.util.stream.BinaryWriter;
import com.avenga.steamclient.util.stream.MemoryStream;
import com.avenga.steamclient.enums.SeekOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Arrays;
import java.util.zip.CRC32;

public class CryptoHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoHelper.class);

    private static final int CRYPTO_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ALGORITHM_WITHOUT_PADDING = "AES/ECB/NoPadding";
    private static final String ALGORITHM_WITH_PADDING = "AES/CBC/PKCS7Padding";

    public static final String SEC_PROV;

    static {
        try {
            if (Utils.getOSType() == EOSType.AndroidUnknown) {
                @SuppressWarnings("unchecked")
                Class<? extends Provider> provider =
                        (Class<? extends Provider>) Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
                Security.insertProviderAt(provider.getDeclaredConstructor().newInstance(), 1);
                SEC_PROV = "SC";
            } else {
                @SuppressWarnings("unchecked")
                Class<? extends Provider> provider =
                        (Class<? extends Provider>) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                Security.addProvider(provider.getDeclaredConstructor().newInstance());
                SEC_PROV = "BC";
            }
        } catch (Exception e) {
            throw new SecurityException("Couldn't create security provider", e);
        }
    }

    /**
     * Generate an array of random bytes given the input length
     *
     * @param size the size of the block to generate
     * @return the generated block
     */
    public static byte[] generateRandomBlock(int size) {
        SecureRandom random = new SecureRandom();
        byte[] b = new byte[size];
        random.nextBytes(b);
        return b;
    }

    /**
     * Performs CRC32 on an input byte array using the CrcStandard.Crc32Bit parameters
     *
     * @param input array to hash
     * @return the hashed result
     */
    public static byte[] crcHash(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        CRC32 crc = new CRC32();
        crc.update(input);
        final long hash = crc.getValue();
        MemoryStream ms = new MemoryStream(4);
        BinaryWriter bw = new BinaryWriter(ms.asOutputStream());

        try {
            bw.writeInt((int) hash);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return ms.toByteArray();
    }

    /**
     * Decrypts using AES/CBC/PKCS7 with an input byte array and key, using the random IV prepended using AES/ECB/None
     *
     * @param input array to decrypt
     * @param key   encryption key
     * @return decrypted message
     * @throws CryptoException deception while encrypting
     */
    public static byte[] symmetricDecrypt(byte[] input, byte[] key) throws CryptoException {
        return symmetricDecrypt(input, key, new Passable<>());
    }

    /**
     * Decrypts using AES/CBC/PKCS7 with an input byte array and key, using the random IV prepended using AES/ECB/None
     *
     * @param input array to decrypt
     * @param key   encryption key
     * @param iv    the random IV
     * @return decrypted message
     * @throws CryptoException deception while encrypting
     */
    public static byte[] symmetricDecrypt(byte[] input, byte[] key, Passable<byte[]> iv) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        try {

            if (key.length != CRYPTO_KEY_LENGTH) {
                LOGGER.debug(String.format("SymmetricDecrypt used with non %d byte key!", CRYPTO_KEY_LENGTH));
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM_WITHOUT_PADDING, SEC_PROV);

            // first 16 bytes of input is the ECB encrypted IV
            iv.setValue(new byte[IV_LENGTH]);
            final byte[] cryptedIv = Arrays.copyOfRange(input, 0, IV_LENGTH);

            // the rest is ciphertext
            byte[] cipherText = new byte[input.length - cryptedIv.length];
            cipherText = Arrays.copyOfRange(input, cryptedIv.length, cryptedIv.length + cipherText.length);

            // decrypt the IV using ECB
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ENCRYPTION_ALGORITHM));
            iv.setValue(cipher.doFinal(cryptedIv));

            cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING, SEC_PROV);

            // decrypt the remaining ciphertext in cbc with the decrypted IV
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ENCRYPTION_ALGORITHM), new IvParameterSpec(iv.getValue()));
            return cipher.doFinal(cipherText);
        } catch (final InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
            throw new CryptoException("failed to symmetric decrypt", e);
        }
    }

    /**
     * Performs an encryption using AES/CBC/PKCS7 with an input byte array and key, with a random IV prepended using AES/ECB/None
     *
     * @param input array to encrypt
     * @param key   encryption key
     * @param iv    the random IV
     * @return encrypted message
     * @throws CryptoException exception while encrypting
     */
    public static byte[] symmetricEncryptWithIV(byte[] input, byte[] key, byte[] iv) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        if (iv == null) {
            throw new IllegalArgumentException("iv is null");
        }

        try {

            if (key.length != CRYPTO_KEY_LENGTH) {
                LOGGER.debug(String.format("SymmetricEncrypt used with non %d byte key!", CRYPTO_KEY_LENGTH));
            }

            // encrypt iv using ECB and provided key
            Cipher cipher = Cipher.getInstance(ALGORITHM_WITHOUT_PADDING, SEC_PROV);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ENCRYPTION_ALGORITHM));

            final byte[] cryptedIv = cipher.doFinal(iv);

            // encrypt input plaintext with CBC using the generated (plaintext) IV and the provided key
            cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING, SEC_PROV);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ENCRYPTION_ALGORITHM), new IvParameterSpec(iv));

            final byte[] cipherText = cipher.doFinal(input);

            // final output is 16 byte ecb crypted IV + cbc crypted plaintext
            final byte[] output = new byte[cryptedIv.length + cipherText.length];
            System.arraycopy(cryptedIv, 0, output, 0, cryptedIv.length);
            System.arraycopy(cipherText, 0, output, cryptedIv.length, cipherText.length);

            return output;
        } catch (final InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                IllegalBlockSizeException | NoSuchPaddingException | NoSuchProviderException | BadPaddingException e) {
            throw new CryptoException("failed to symmetric encrypt", e);
        }
    }

    /**
     * Performs an encryption using AES/CBC/PKCS7 with an input byte array and key, with a random IV prepended using AES/ECB/None
     *
     * @param input array to encrypt
     * @param key   encryption key
     * @return encrypted message
     * @throws CryptoException exception while encrypting
     */
    public static byte[] symmetricEncrypt(byte[] input, byte[] key) throws CryptoException {
        return symmetricEncryptWithIV(input, key, generateRandomBlock(IV_LENGTH));
    }

    /**
     * Decrypts using AES/CBC/PKCS7 with an input byte array and key, using the IV (comprised of random bytes and the
     * HMAC-SHA1 of the random bytes and plaintext) prepended using AES/ECB/None
     *
     * @param input      array to decrypt
     * @param key        encryption key
     * @param hmacSecret the IV
     * @return decrypted message
     * @throws CryptoException exception while decrypting
     */
    public static byte[] symmetricDecryptHMACIV(byte[] input, byte[] key, byte[] hmacSecret) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        if (hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is null");
        }

        if (key.length < IV_LENGTH) {
            LOGGER.debug(String.format("symmetricDecryptHMACIV used with shorter than %d byte key!", IV_LENGTH));
        }

        byte[] truncatedKeyForHmac = new byte[IV_LENGTH];
        System.arraycopy(key, 0, truncatedKeyForHmac, 0, truncatedKeyForHmac.length);

        Passable<byte[]> iv = new Passable<>(new byte[IV_LENGTH]);
        byte[] plaintextData = symmetricDecrypt(input, key, iv);

        // validate HMAC
        byte[] hmacBytes;

        MemoryStream ms = new MemoryStream();
        ms.write(iv.getValue(), iv.getValue().length - 3, 3);
        ms.write(plaintextData, 0, plaintextData.length);
        ms.seek(0, SeekOrigin.BEGIN);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA1"));
            hmacBytes = mac.doFinal(ms.toByteArray());

            for (int i = 0; i < iv.getValue().length - 3; i++) {
                if (hmacBytes[i] != iv.getValue()[i]) {
                    throw new CryptoException("NetFilterEncryption was unable to decrypt packet: HMAC from server did not match computed HMAC.");
                }
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException("NetFilterEncryption was unable to decrypt packet", e);
        }

        return plaintextData;
    }

    /**
     * Performs an encryption using AES/CBC/PKCS7 with an input byte array and key, with a IV (comprised of random bytes
     * and the HMAC-SHA1 of the random bytes and plaintext) prepended using AES/ECB/None
     *
     * @param input      array to encrypt
     * @param key        encryption key
     * @param hmacSecret the IV
     * @return encrypted message
     * @throws CryptoException exception while encrypting
     */
    public static byte[] symmetricEncryptWithHMACIV(byte[] input, byte[] key, byte[] hmacSecret) throws CryptoException {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        if (hmacSecret == null) {
            throw new IllegalArgumentException("hmacSecret is null");
        }

        // IV is HMAC-SHA1(Random(3) + Plaintext) + Random(3). (Same random values for both)
        byte[] iv = new byte[IV_LENGTH];
        byte[] random = generateRandomBlock(3);
        System.arraycopy(random, 0, iv, iv.length - random.length, random.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(random, 0, random.length);
        baos.write(input, 0, input.length);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA1"));
            byte[] hash = mac.doFinal(baos.toByteArray());

            System.arraycopy(hash, 0, iv, 0, iv.length - random.length);

            return symmetricEncryptWithIV(input, key, iv);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CryptoException("NetFilterEncryption was unable to decrypt packet", e);
        }
    }
}
