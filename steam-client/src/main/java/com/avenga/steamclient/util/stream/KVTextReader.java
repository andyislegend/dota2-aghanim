package com.avenga.steamclient.util.stream;

import com.avenga.steamclient.model.KeyValue;
import com.avenga.steamclient.model.Passable;
import com.avenga.steamclient.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.*;

public class KVTextReader extends PushbackInputStream {

    public static final Map<Character, Character> ESCAPED_MAPPING;

    static {
        Map<Character, Character> escapedMapping = new TreeMap<>();
        escapedMapping.put('n', '\n');
        escapedMapping.put('r', '\r');
        escapedMapping.put('t', '\t');
        escapedMapping.put('\\', '\\');

        ESCAPED_MAPPING = Collections.unmodifiableMap(escapedMapping);
    }

    public KVTextReader(KeyValue keyValue, InputStream is) throws IOException {
        super(is);
        Passable<Boolean> wasQuoted = new Passable<>(false);
        Passable<Boolean> wasConditional = new Passable<>(false);

        KeyValue currentKey = keyValue;

        do {
            String token = readToken(wasQuoted, wasConditional);

            if (StringUtils.isNullOrEmpty(token)) {
                break;
            }

            if (currentKey == null) {
                currentKey = new KeyValue(token);
            } else {
                currentKey.setName(token);
            }

            token = readToken(wasQuoted, wasConditional);

            if (wasConditional.getValue()) {
                // Now get the '{'
                token = readToken(wasQuoted, wasConditional);
            }

            if (token.startsWith("{") && !wasQuoted.getValue()) {
                // header is valid so load the file
                this.recursiveLoadFromBuffer(currentKey, currentKey.getChildren());
            } else {
                throw new IllegalStateException("LoadFromBuffer: missing {");
            }

//            currentKey = null;
        } while (!endOfStream());
    }

    private void eatWhiteSpace() throws IOException {
        while (!endOfStream()) {
            if (!Character.isWhitespace((char) peek())) {
                break;
            }

            read();
        }
    }

    private boolean eatCPPComment() throws IOException {
        if (!endOfStream()) {
            char next = (char) peek();

            if (next == '/') {
                readLine();
                return true;
                /*
                 *  As came up in parsing the Dota 2 units.txt file, the reference (Valve) implementation
                 *  of the KV format considers a single forward slash to be sufficient to comment out the
                 *  entirety of a line. While they still _tend_ to use two, it's not required, and likely
                 *  is just done out of habit.
                 */
            }

            return false;
        }
        return false;
    }

    private void readLine() throws IOException {
        char character;
        do {
            character = (char) read();
        } while (character != '\n' && !endOfStream());
    }

    private byte peek() throws IOException {
        int byteFromStream = read();
        if (byteFromStream >= 0) {
            unread(byteFromStream);
        }
        return (byte) byteFromStream;
    }

    public String readToken(Passable<Boolean> wasQuoted, Passable<Boolean> wasConditional) throws IOException {
        wasQuoted.setValue(false);
        wasConditional.setValue(false);

        while (true) {
            eatWhiteSpace();

            if (endOfStream()) {
                return null;
            }

            if (!eatCPPComment()) {
                break;
            }
        }

        if (endOfStream()) {
            return null;
        }

        char next = (char) peek();
        if (next == '"') {
            wasQuoted.setValue(true);

            // "
            read();

            StringBuilder stringBuilder = new StringBuilder();

            while (!endOfStream()) {
                if (peek() == '\\') {
                    read();

                    char escapedChar = (char) read();

                    Character replacedChar = ESCAPED_MAPPING.get(escapedChar);
                    if (replacedChar == null) {
                        replacedChar = escapedChar;
                    }

                    stringBuilder.append(replacedChar);

                    continue;
                }

                if (peek() == '"') {
                    break;
                }

                stringBuilder.append((char) read());
            }

            // "
            read();

            return stringBuilder.toString();
        }

        if (next == '{' || next == '}') {
            read();
            return String.valueOf(next);
        }

        boolean bConditionalStart = false;
        int count = 0;
        StringBuilder ret = new StringBuilder();

        while (!endOfStream()) {
            next = (char) peek();

            if (next == '"' || next == '{' || next == '}') {
                break;
            }

            if (next == '[') {
                bConditionalStart = true;
            }

            if (next == ']' && bConditionalStart) {
                wasConditional.setValue(true);
            }

            if (Character.isWhitespace(next)) {
                break;
            }

            if (count < 1023) {
                ret.append(next);
            } else {
                throw new IOException("ReadToken overflow");
            }

            read();
        }
        return ret.toString();
    }

    private boolean endOfStream() {
        try {
            return peek() == -1;
        } catch (IOException e) {
            return true;
        }
    }

    void recursiveLoadFromBuffer(KeyValue keyValue, List<KeyValue> children) throws IOException {
        Passable<Boolean> wasQuoted = new Passable<>(false);
        Passable<Boolean> wasConditional = new Passable<>(false);

        while (true) {
            // get the key name
            String name = this.readToken(wasQuoted, wasConditional);

            if (StringUtils.isNullOrEmpty(name)) {
                throw new IllegalStateException("RecursiveLoadFromBuffer: got EOF or empty keyname");
            }

            if (name.startsWith("}") && !wasQuoted.getValue()) {
                break;
            }

            KeyValue dat = new KeyValue(name);
            dat.setChildren(new ArrayList<>());
            children.add(dat);

            String value = this.readToken(wasQuoted, wasConditional);

            Objects.requireNonNull(value, "RecursiveLoadFromBuffer: value wasn't provided");

            if (value.startsWith("}") && !wasQuoted.getValue()) {
                throw new IllegalStateException("RecursiveLoadFromBuffer:  got } in key");
            }

            if (value.startsWith("{") && !wasQuoted.getValue()) {
                this.recursiveLoadFromBuffer(keyValue, dat.getChildren());
            } else {
                if (wasConditional.getValue()) {
                    throw new IllegalStateException("RecursiveLoadFromBuffer:  got conditional between key and value");
                }

                dat.setValue(value);
            }
        }
    }
}
