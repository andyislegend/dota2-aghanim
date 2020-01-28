package com.avenga.steamclient.model;

import com.avenga.steamclient.util.stream.KVTextReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeyValue {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyValue.class);

    /**
     * Represents an invalid {@link KeyValue} given when a searched for child does not exist.
     */
    public static final KeyValue INVALID = new KeyValue();

    private String name;

    private String value;

    private List<KeyValue> children = new ArrayList<>();

    public KeyValue(String name) {
        this.name = name;
    }

    /**
     * Gets the child {@link KeyValue} with the specified key.
     * If no child with the given key exists, {@link KeyValue#INVALID} is returned.
     *
     * @param key key
     * @return the child {@link KeyValue}
     */
    public KeyValue get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        return children.stream().filter(child -> key.equalsIgnoreCase(child.name)).findFirst().orElse(INVALID);
    }

    /**
     * Attempts to convert and return the value of this instance as an integer.
     * If the conversion is invalid, the default value is returned.
     *
     * @param defaultValue The default value to return if the conversion is invalid.
     * @return The value of this instance as an unsigned byte.
     */
    public int asInteger(int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NullPointerException | NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public boolean readAsText(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("input stream is null");
        }

        children = new ArrayList<>();

        new KVTextReader(this, is);

        return true;
    }
}
