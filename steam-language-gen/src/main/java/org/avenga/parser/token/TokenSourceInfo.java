package org.avenga.parser.token;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenSourceInfo {
    private String fileName;
    private int startLineNumber;
    private int startColumnNumber;
    private int endLineNumber;
    private int endColumnNumber;
}
