package org.avenga.parser.node;

import lombok.Getter;
import lombok.Setter;
import org.avenga.parser.symbol.Symbol;

@Getter
@Setter
public class ClassNode extends Node {
    private Symbol ident;
    private Symbol parent;
    private boolean emit;
}
