package com.avenga.steamclient.base;

import com.avenga.steamclient.enums.EMsg;

public interface SteamSerializableHeader extends SteamSerializable {
    void setEMsg(EMsg msg);
}
