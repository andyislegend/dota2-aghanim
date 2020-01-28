package com.avenga.steamclient.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class NetworkUtils {

    public static InetAddress getIPAddress(int ipAddr) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(ipAddr);

        byte[] result = byteBuffer.array();

        try {
            return InetAddress.getByAddress(result);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
