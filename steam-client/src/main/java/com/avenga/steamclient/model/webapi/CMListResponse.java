package com.avenga.steamclient.model.webapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CMListResponse {
    private Response response;

    @Getter
    @Setter
    public static class Response {

        @JsonProperty("serverlist")
        List<String> socketServerList;

        @JsonProperty("serverlist_websockets")
        List<String> websocketServerList;

        Integer result;

        String message;
    }
}
