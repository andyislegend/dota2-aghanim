package com.avenga.steamclient.util;

import java.util.Optional;
import java.util.function.Function;

public class CallbackHandlerUtils {

    public static <T, R> Optional<R> getValueOrDefault(Optional<T> value, Function<T, R> protoMapper) {
        return value.isPresent() ? Optional.of(protoMapper.apply(value.get())) : Optional.empty();
    }
}
