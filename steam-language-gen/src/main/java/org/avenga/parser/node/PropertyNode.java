package org.avenga.parser.node;

import lombok.Getter;
import lombok.Setter;
import org.avenga.parser.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PropertyNode extends Node {

    private String flags;
    private String flagsOpt;
    private Symbol type;
    private List<Symbol> defaultSymbols = new ArrayList<>();
    private String obsolete;
    private boolean emit = true;
}
