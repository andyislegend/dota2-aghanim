package com.avenga.steamclient.crypto;

import com.avenga.steamclient.exception.BerDecodeException;

import java.math.BigInteger;
import java.util.List;

public class AsnKeyParser {

    private final AsnParser asnParser;

    AsnKeyParser(List<Byte> contents) {
        asnParser = new AsnParser(contents);
    }

    public static byte[] trimLeadingZero(byte[] values) {
        byte[] r;
        if (0x00 == values[0] && values.length > 1) {
            r = new byte[values.length - 1];
            System.arraycopy(values, 1, r, 0, values.length - 1);
        } else {
            r = new byte[values.length];
            System.arraycopy(values, 0, r, 0, values.length);
        }

        return r;
    }

    public static boolean equalOid(byte[] first, byte[] second) {
        if (first.length != second.length) {
            return false;
        }

        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                return false;
            }
        }

        return true;
    }

    public BigInteger[] parseRSAPublicKey() throws BerDecodeException {
        final BigInteger[] parameters = new BigInteger[2];

        // Current value
        // Sanity Check
        // Checkpoint
        int position = asnParser.currentPosition();

        // Ignore Sequence - PublicKeyInfo
        int length = asnParser.nextSequence();
        if (length != asnParser.remainingBytes()) {
            throw new BerDecodeException(String.format("Incorrect Sequence Size. Specified: %d, Remaining: %d",
                    length, asnParser.remainingBytes()), position);
        }

        // Checkpoint
        position = asnParser.currentPosition();

        // Ignore Sequence - AlgorithmIdentifier
        length = asnParser.nextSequence();
        if (length > asnParser.remainingBytes()) {
            throw new BerDecodeException(String.format("Incorrect AlgorithmIdentifier Size. Specified: %d, Remaining: %d",
                    length, asnParser.remainingBytes()), position);
        }

        // Checkpoint
        position = asnParser.currentPosition();
        // Grab the OID
        final byte[] value = asnParser.nextOID();
        final byte[] oid = {(byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01, (byte) 0x01};
        if (!equalOid(value, oid)) {
            throw new BerDecodeException("Expected OID 1.2.840.113549.1.1.1", position);
        }

        // Optional Parameters
        if (asnParser.isNextNull()) {
            asnParser.nextNull();
            // Also OK: value = asnParser.Next();
        } else {
            // Gracefully skip the optional data
            asnParser.next();
        }

        // Checkpoint
        position = asnParser.currentPosition();

        // Ignore BitString - PublicKey
        length = asnParser.nextBitString();
        if (length > asnParser.remainingBytes()) {
            throw new BerDecodeException(String.format("Incorrect PublicKey Size. Specified: %d, Remaining: %d",
                    length, asnParser.remainingBytes()), position);
        }

        // Checkpoint
        position = asnParser.currentPosition();

        // Ignore Sequence - RSAPublicKey
        length = asnParser.nextSequence();
        if (length < asnParser.remainingBytes()) {
            throw new BerDecodeException(String.format("Incorrect RSAPublicKey Size. Specified: %d, Remaining: %d",
                    length, asnParser.remainingBytes()), position);
        }

        parameters[0] = new BigInteger(1, trimLeadingZero(asnParser.nextInteger()));
        parameters[1] = new BigInteger(1, trimLeadingZero(asnParser.nextInteger()));

        assert 0 == asnParser.remainingBytes();

        return parameters;
    }
}
