package org.avenga.parser.symbol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.avenga.parser.node.Node;

@Getter
@Setter
@AllArgsConstructor
public class StrongSymbol implements Symbol {
    private Node clazz;
    private Node property;

    public StrongSymbol(Node clazz) {
        this.clazz = clazz;
    }
}
