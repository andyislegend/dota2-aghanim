package com.avenga.steamclient.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;

@Getter
@AllArgsConstructor
public enum ProtocolType {

    TCP(1),
    UDP(1 << 1),
    WEB_SOCKET(1 << 2);

    public static final EnumSet<ProtocolType> ALL = EnumSet.of(TCP, UDP, WEB_SOCKET);

    private final int code;

    public static EnumSet<ProtocolType> from(int code) {
        EnumSet<ProtocolType> set = EnumSet.noneOf(ProtocolType.class);
        for (ProtocolType e : ProtocolType.values()) {
            if ((e.code & code) == e.code) {
                set.add(e);
            }
        }
        return set;
    }

    public static int code(EnumSet<ProtocolType> flags) {
        int code = 0;
        for (ProtocolType flag : flags) {
            code |= flag.code;
        }
        return code;
    }
}
