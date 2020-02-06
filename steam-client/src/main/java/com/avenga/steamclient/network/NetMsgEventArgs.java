package com.avenga.steamclient.network;

import com.avenga.steamclient.event.EventArgs;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
@AllArgsConstructor
public class NetMsgEventArgs extends EventArgs {

    private byte[] data;
    private InetSocketAddress endPoint;

    public NetMsgEventArgs withData(byte[] data) {
        return new NetMsgEventArgs(data, this.endPoint);
    }
}
