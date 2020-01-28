package com.avenga.steamclient.enums;

import java.util.Arrays;

public enum KeyValueType {
    UNKNOWN(Byte.MIN_VALUE),
    NONE((byte) 0),
    STRING((byte) 1),
    INT32((byte) 2),
    FLOAT32((byte) 3),
    POINTER((byte) 4),
    WIDESTRING((byte) 5),
    COLOR((byte) 6),
    UINT64((byte) 7),
    END((byte) 8),
    INT64((byte) 10);

    private byte code;

    KeyValueType(byte code) {
        this.code = code;
    }

    public byte code() {
        return this.code;
    }

    public static KeyValueType from(byte code) {
        return Arrays.stream(KeyValueType.values())
                .filter(type -> type.code == code)
                .findFirst().orElse(UNKNOWN);
    }
}
