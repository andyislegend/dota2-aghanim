package org.avenga.parser.node;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Node {
    private List<Node> childNodes = new ArrayList<>();
    private String name;
}
