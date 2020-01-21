package com.avenga.steamclient.util;

import com.avenga.steamclient.util.compare.ObjectsCompat;

import java.util.Map;

public class CollectionUtils {
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (ObjectsCompat.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
