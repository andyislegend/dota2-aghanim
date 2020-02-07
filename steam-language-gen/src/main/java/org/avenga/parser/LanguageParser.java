package org.avenga.parser;

import org.avenga.parser.token.Token;
import org.avenga.parser.token.TokenSourceInfo;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

public class LanguageParser {

    private static final Pattern PATTERN = Pattern.compile("(?<whitespace>\\s+)|" +
            "(?<terminator>[;])|" +
            "[\"](?<string>.+?)[\"]|" +
            "//(?<comment>.*)$|" +
            "(?<identifier>-?[a-zA-Z_0-9][a-zA-Z0-9_:.]*)|" +
            "[#](?<preprocess>[a-zA-Z]*)|" +
            "(?<operator>[{}<>\\]=|])|" +
            "(?<invalid>[^\\s]+)", Pattern.MULTILINE);

    private static final List<String> GROUP_NAMES = Arrays.asList("whitespace", "terminator", "string", "comment",
            "identifier", "preprocess", "operator", "invalid");

    public static Queue<Token> tokenizeString(String buffer, String fileName) {
        var bufferLines = buffer.split("[\\r\\n]+");
        var tokens = new ArrayDeque<Token>();

        for (int i = 0; i < bufferLines.length; i++) {
            var line = bufferLines[i];
            var matcher = PATTERN.matcher(line);

            while (matcher.find()) {
                String matchValue = null;
                String groupName = null;

                for (String tempName : GROUP_NAMES) {
                    matchValue = matcher.group(tempName);
                    groupName = tempName;
                    if (matchValue != null) {
                        break;
                    }
                }

                if (matchValue == null || "comment".equals(groupName) || "whitespace".equals(groupName)) {
                    continue;
                }

                var startColumnNumber = line.indexOf(matchValue);
                var endColumnNumber = line.indexOf(matchValue) + matchValue.length();
                var source = new TokenSourceInfo(fileName, i, startColumnNumber, i, endColumnNumber);
                var token = new Token(groupName, matchValue, source);

                tokens.add(token);
            }
        }

        return tokens;
    }
}
