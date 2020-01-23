package com.avenga.steamclient.util;

import com.avenga.steamclient.enums.EOSType;
import org.apache.commons.lang3.SystemUtils;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Utils {

    private static final String JAVA_RUNTIME = getSystemProperty("java.runtime.name");
    private static final int START_OFFSET = 0;

    public static EOSType getOSType() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return EOSType.WinUnknown;
        }
        if (SystemUtils.IS_OS_MAC) {
            return EOSType.MacOSUnknown;
        }
        if (JAVA_RUNTIME != null && JAVA_RUNTIME.startsWith("Android")) {
            return EOSType.AndroidUnknown;
        }
        if (SystemUtils.IS_OS_LINUX) {
            return EOSType.LinuxUnknown;
        }
        return EOSType.Unknown;
    }

    private static boolean checkOS(String namePrefix, String versionPrefix) {
        return SystemUtils.OS_NAME.startsWith(namePrefix) && SystemUtils.OS_VERSION.startsWith(versionPrefix);
    }

    private static String getSystemProperty(final String property) {
        try {
            return System.getProperty(property);
        } catch (final SecurityException ex) {
            // we are not allowed to look at this property
            return null;
        }
    }

    /**
     * Convenience method for calculating the CRC2 checksum of a string.
     *
     * @param s the string
     * @return long value of the CRC32
     */
    public static long crc32(String s) {
        Checksum checksum = new CRC32();
        byte[] bytes = s.getBytes();
        checksum.update(bytes, START_OFFSET, bytes.length);
        return checksum.getValue();
    }
}
