package com.avenga.steamclient.util.compare;

public class ObjectsCompat {
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}