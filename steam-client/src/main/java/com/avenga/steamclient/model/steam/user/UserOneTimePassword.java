package com.avenga.steamclient.model.steam.user;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOneTimePassword {
    private int type;
    private String identifier;
    private byte[] sharedSecret;
    private int timeDrift;
}
