package org.avenga.parser.token;

import org.apache.commons.io.IOUtils;
import org.avenga.parser.LanguageParser;
import org.avenga.parser.node.ClassNode;
import org.avenga.parser.node.EnumNode;
import org.avenga.parser.node.Node;
import org.avenga.parser.node.PropertyNode;
import org.avenga.parser.symbol.SymbolLocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;

public class TokenAnalyzer {
    public static Node analyze(Queue<Token> tokens, String dir) throws IOException {
        var root = new Node();

        while (!tokens.isEmpty()) {
            var currentToken = tokens.poll();

            switch (currentToken.getName()) {
                case "EOF":
                    break;
                case "preprocess":
                    var text = expect(tokens, "string");

                    if ("import".equals(currentToken.getValue())) {
                        Queue<Token> parentTokens = LanguageParser.tokenizeString(IOUtils.toString(
                                new FileInputStream(new File(dir + "/" + text.getValue())), StandardCharsets.UTF_8),
                                text.getValue());
                        Node newRoot = analyze(parentTokens, dir);
                        newRoot.getChildNodes().forEach(child -> root.getChildNodes().add(child));
                    }
                    break;
                case "identifier":
                    Token name, op1;
                    switch (currentToken.getValue()) {
                        case "class":
                            name = expect(tokens, "identifier");
                            Token ident = null;
                            Token parent = null;

                            op1 = optional(tokens, "operator", "<");
                            if (op1 != null) {
                                ident = expect(tokens, "identifier");
                                expect(tokens, "operator", ">");
                            }

                            var expectToken = optional(tokens, "identifier", "expects");
                            if (expectToken != null) {
                                parent = expect(tokens, "identifier");
                            }

                            var removed = optional(tokens, "identifier", "removed");
                            if (removed != null) {
                                optional(tokens, "string");
                                optional(tokens, "terminator");
                            }

                            var classNode = new ClassNode();
                            classNode.setName(name.getValue());

                            if (ident != null) {
                                classNode.setIdent(SymbolLocator.lookupSymbol(root, ident.getValue(), false));
                            }

                            if (parent != null) {
                                classNode.setParent(SymbolLocator.lookupSymbol(root, parent.getValue(), true));
                            }

                            classNode.setEmit(removed == null);

                            root.getChildNodes().add(classNode);
                            parseInnerScope(tokens, classNode, root);
                            break;
                        case "enum":
                            name = expect(tokens, "identifier");
                            Token datatype = null;

                            op1 = optional(tokens, "operator", "<");
                            if (op1 != null) {
                                datatype = expect(tokens, "identifier");
                                expect(tokens, "operator", ">");
                            }

                            var flag = optional(tokens, "identifier", "flags");
                            var enumNode = new EnumNode();
                            enumNode.setName(name.getValue());

                            if (flag != null) {
                                enumNode.setFlag(flag.getValue());
                            }

                            if (datatype != null) {
                                enumNode.setType(SymbolLocator.lookupSymbol(root, datatype.getValue(), false));
                            }

                            root.getChildNodes().add(enumNode);
                            parseInnerScope(tokens, enumNode, root);
                            break;
                    }
            }
        }

        return root;
    }

    private static void parseInnerScope(Queue<Token> tokens, Node parent, Node root) {
        expect(tokens, "operator", "{");
        var scope2 = optional(tokens, "operator", "}");

        while (scope2 == null) {
            var propertyNode = new PropertyNode();
            var firstToken = tokens.poll();
            var firstTokenOperator = optional(tokens, "operator", "<");
            Token flagOperator = null;

            if (firstTokenOperator != null) {
                flagOperator = expect(tokens, "identifier");
                expect(tokens, "operator", ">");

                propertyNode.setFlagsOpt(flagOperator.getValue());
            }

            var secondToken = optional(tokens, "identifier");
            var thirdToken = optional(tokens, "identifier");

            if (thirdToken != null) {
                propertyNode.setName(thirdToken.getValue());
                propertyNode.setType(SymbolLocator.lookupSymbol(root, secondToken.getValue(), false));
                propertyNode.setFlags(firstToken.getValue());
            } else if (secondToken != null) {
                propertyNode.setName(secondToken.getValue());
                propertyNode.setType(SymbolLocator.lookupSymbol(root, firstToken.getValue(), false));
            } else {
                propertyNode.setName(firstToken.getValue());
            }

            var defop = optional(tokens, "operator", "=");

            if (defop != null) {
                while (true) {
                    var value = tokens.poll();
                    propertyNode.getDefaultSymbols().add(SymbolLocator.lookupSymbol(root, value.getValue(), false));

                    if (optional(tokens, "operator", "|") != null) {
                        continue;
                    }

                    expect(tokens, "terminator", ";");
                    break;
                }
            } else {
                expect(tokens, "terminator", ";");
            }

            var obsolete = optional(tokens, "identifier", "obsolete");
            if (obsolete != null) {
                propertyNode.setObsolete("");
                var obsoleteReason = optional(tokens, "string");

                if (obsoleteReason != null)
                    propertyNode.setObsolete(obsoleteReason.getValue());
            }

            var removed = optional(tokens, "identifier", "removed");
            if (removed != null) {
                propertyNode.setEmit(false);
                optional(tokens, "string");
                optional(tokens, "terminator");
            }

            parent.getChildNodes().add(propertyNode);
            scope2 = optional(tokens, "operator", "}");
        }
    }

    private static Token expect(Queue<Token> tokens, String name) {
        var peek = tokens.peek();

        if (peek == null) {
            return new Token("EOF", "");
        }

        if (!peek.getName().equals(name)) {
            throw new IllegalStateException("Expecting " + name);
        }

        return tokens.poll();
    }

    private static Token expect(Queue<Token> tokens, String name, String value) {
        var peek = tokens.peek();

        if (peek == null) {
            return new Token("EOF", "");
        }

        if (!peek.getName().equals(name) || !peek.getValue().equals(value)) {
            if (peek.getSource() != null) {
                var source = peek.getSource();
                throw new IllegalStateException("Expecting " + name + "" + value +", but got " + peek.getValue() +
                        " at " + source.getFileName() + " " + source.getStartLineNumber() +", " + source.getEndColumnNumber() +
                        "-" + source.getEndLineNumber() + ", " + source.getEndColumnNumber());
            } else {
                throw new IllegalStateException("Expecting " + name + "" + value +", but got " + peek.getValue());
            }
        }

        return tokens.poll();
    }

    private static Token optional(Queue<Token> tokens, String name) {
        var peek = tokens.peek();

        if (peek == null) {
            return new Token("EOF", "");
        }

        if (!peek.getName().equals(name)) {
            return null;
        }

        return tokens.poll();
    }

    private static Token optional(Queue<Token> tokens, String name, String value) {
        var peek = tokens.peek();

        if (peek == null) {
            return new Token("EOF", "");
        }

        if (!peek.getName().equals(name)  || !peek.getValue().equals(value)) {
            return null;
        }

        return tokens.poll();
    }
}
