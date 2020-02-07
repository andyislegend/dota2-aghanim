package com.avenga.steamclient.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SteamSerializable {

    void serialize(OutputStream stream) throws IOException;

    void deserialize(InputStream stream) throws IOException;
}
