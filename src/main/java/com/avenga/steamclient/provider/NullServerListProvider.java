package com.avenga.steamclient.provider;

import com.avenga.steamclient.model.discovery.ServerRecord;

import java.util.List;

public class NullServerListProvider implements ServerListProvider {

    @Override
    public List<ServerRecord> fetchServerList() {
        return null;
    }

    @Override
    public void updateServerList(List<ServerRecord> endpoints) {

    }
}
