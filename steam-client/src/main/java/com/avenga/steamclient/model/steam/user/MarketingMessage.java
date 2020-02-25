package com.avenga.steamclient.model.steam.user;

import com.avenga.steamclient.enums.EMarketingMessageFlags;
import com.avenga.steamclient.model.GlobalID;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;

@Getter
@Setter
public class MarketingMessage {
    private GlobalID id;
    private String url;
    private EnumSet<EMarketingMessageFlags> flags;
}
