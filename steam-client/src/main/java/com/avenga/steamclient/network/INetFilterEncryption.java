package com.avenga.steamclient.network;

public interface INetFilterEncryption {

    byte[] processIncoming(byte[] data);
    byte[] processOutgoing(byte[] data);
}
