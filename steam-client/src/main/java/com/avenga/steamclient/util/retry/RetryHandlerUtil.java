package com.avenga.steamclient.util.retry;

import com.avenga.steamclient.exception.CallbackTimeoutException;
import com.avenga.steamclient.model.steam.SteamMessageCallback;
import com.avenga.steamclient.steam.client.SteamClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.avenga.steamclient.constant.Constant.RETRY_EXCEPTION_MESSAGE_FORMAT;

public class RetryHandlerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandlerUtil.class);

    public static <T, R> R getOrRetry(SupplierCallbackHandler<R> supplier, SteamMessageCallback<T> messageCallback,int retryCount,
                                    SteamClient client) throws CallbackTimeoutException {
        var count = 1;
        var errorMessage = "";
        while (count <= retryCount) {
            try {
                return supplier.get();
            } catch (CallbackTimeoutException e) {
                LOGGER.debug("{}: Consumer messageCallback handler retry count {} with error: {}",
                        client.getClientName(), count, e.toString());
                count++;
                errorMessage = e.toString();
            }
        }
        client.removeCallbackFromQueue(messageCallback);
        throw new CallbackTimeoutException(String.format(RETRY_EXCEPTION_MESSAGE_FORMAT, retryCount, errorMessage));
    }
}
