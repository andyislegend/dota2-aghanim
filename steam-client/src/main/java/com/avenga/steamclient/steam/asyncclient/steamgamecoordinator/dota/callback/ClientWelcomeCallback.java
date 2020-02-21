package com.avenga.steamclient.steam.asyncclient.steamgamecoordinator.dota.callback;

import com.avenga.steamclient.protobufs.dota.GCSdkGCMessages.CMsgClientWelcome;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

@Getter
public class ClientWelcomeCallback extends BaseCallbackMessage {
    private int version;
    private String saveGameKey;
    private int itemSchemaCrc;
    private String itemsGameUrl;
    private int gcSocacheFileVersion;
    private String txnCountryCode;

    public ClientWelcomeCallback(CMsgClientWelcome.Builder builder) {
        this.version = builder.getVersion();
        this.saveGameKey = builder.getSaveGameKey().toStringUtf8();
        this.itemSchemaCrc = builder.getItemSchemaCrc();
        this.itemsGameUrl = builder.getItemsGameUrl();
        this.gcSocacheFileVersion = builder.getGcSocacheFileVersion();
        this.txnCountryCode = builder.getTxnCountryCode();
    }
}
