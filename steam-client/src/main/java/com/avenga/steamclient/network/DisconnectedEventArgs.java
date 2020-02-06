package com.avenga.steamclient.network;

import com.avenga.steamclient.event.EventArgs;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DisconnectedEventArgs extends EventArgs {

    private boolean userInitiated;
}
