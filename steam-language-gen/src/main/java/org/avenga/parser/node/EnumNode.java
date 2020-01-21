package org.avenga.parser.node;

import lombok.Getter;
import lombok.Setter;
import org.avenga.parser.symbol.Symbol;

@Getter
@Setter
public class EnumNode extends Node {
    private String flag;
    private Symbol type;
}
