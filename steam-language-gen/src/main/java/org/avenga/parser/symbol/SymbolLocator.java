package org.avenga.parser.symbol;

import org.avenga.parser.node.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SymbolLocator {
    private static final Pattern IDENTIFIER_REGEX = Pattern.compile("(?<identifier>-?[a-zA-Z0-9_:.]*)");
    private static final Pattern FULL_IDENTIFIER_REGEX = Pattern.compile("(?<class>[a-zA-Z0-9_]*?)::(?<name>[a-zA-Z0-9_]*)");

    private static final Map<String, String> WEAK_TYPES = new LinkedHashMap<>() {{
        put("byte", "byte");
        put("short", "Short");
        put("ushort", "Integer");
        put("int", "Integer");
        put("uint", "Long");
        put("long", "Long");
        put("ulong", "Long");

    }};

    public static Symbol lookupSymbol(Node tree, String identifier, boolean strongOnly) {
        var ident = IDENTIFIER_REGEX.matcher(identifier);

        if (!ident.matches()) {
            throw new IllegalArgumentException("Invalid identifier specified " + identifier);
        }

        if (identifier.contains(".")) {
            var split = identifier.split("\\.");
            String val;

            if ("ulong".equals(split[0])) {
                switch (split[1]) {
                    case "MaxValue":
                        return new WeakSymbol("0xFFFFFFFFFFFFFFFF");
                    case "MinValue":
                        return new WeakSymbol("0x0000000000000000");
                    default:
                        return new WeakSymbol(identifier);
                }
            }

            switch (split[1]) {
                case "MaxValue":
                    val = "MAX_VALUE";
                    break;
                case "MinValue":
                    val = "MIN_VALUE";
                    break;
                default:
                    return new WeakSymbol(identifier);
            }

            return new WeakSymbol(WEAK_TYPES.get(split[0]) + "." + val);
        } else if (!identifier.contains("::")) {
            var classNode = findNode(tree, ident.group(0));

            if (classNode == null) {
                if (strongOnly) {
                    throw new IllegalStateException("Invalid weak symbol " + identifier);
                } else {
                    return new WeakSymbol(identifier);
                }
            } else {
                return new StrongSymbol(classNode);
            }
        } else {
            ident = FULL_IDENTIFIER_REGEX.matcher(identifier);

            if (!ident.matches()) {
                throw new IllegalArgumentException("Couldn't parse full identifier " + identifier);
            }

            var classNode = findNode(tree, ident.group("class"));

            if (classNode == null) {
                throw new IllegalStateException("Invalid class in identifier " + identifier);
            }

            var propNode = findNode(classNode, ident.group("name"));

            if (propNode == null) {
                throw new IllegalStateException("Invalid property in identifier " + identifier);
            }

            return new StrongSymbol(classNode, propNode);
        }
    }

    private static Node findNode(Node tree, String symbol) {
        return tree.getChildNodes().stream().filter(child -> child.getName().equals(symbol)).findFirst().orElse(null);
    }
}
