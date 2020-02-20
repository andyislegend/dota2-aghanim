package com.avenga.steamclient.steam.asyncclient.steamuser.callback;

import com.avenga.steamclient.enums.ECurrencyCode;
import com.avenga.steamclient.protobufs.steamclient.SteammessagesClientserver.CMsgClientWalletInfoUpdate;
import com.avenga.steamclient.steam.asyncclient.callbackmanager.BaseCallbackMessage;
import lombok.Getter;

/**
 * This callback is received when wallet info is received from the network.
 */
@Getter
public class WalletInfoCallback extends BaseCallbackMessage {
    private boolean hasWallet;
    private ECurrencyCode currency;
    private int balance;
    private long longBalance;

    public WalletInfoCallback(CMsgClientWalletInfoUpdate.Builder wallet) {
        hasWallet = wallet.getHasWallet();
        currency = ECurrencyCode.from(wallet.getCurrency());
        balance = wallet.getBalance();
        longBalance = wallet.getBalance64();
    }
}
