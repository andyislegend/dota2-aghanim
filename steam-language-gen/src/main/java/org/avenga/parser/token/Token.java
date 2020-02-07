package org.avenga.parser.token;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Token {
    private String name;
    private String value;
    private TokenSourceInfo source;

    public Token(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
