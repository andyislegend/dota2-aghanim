package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;

public interface SteamSerializableMessage extends SteamSerializable {
    EMsg getEMsg();
}
