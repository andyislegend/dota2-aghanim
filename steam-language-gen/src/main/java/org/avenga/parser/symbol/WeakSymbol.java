package org.avenga.parser.symbol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WeakSymbol implements Symbol {
    private String identifier;
}
