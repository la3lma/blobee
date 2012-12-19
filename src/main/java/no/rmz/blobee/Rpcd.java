package no.rmz.blobee;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * An RPC serving demon.  We'll start by it not being very remote.
 * but only procedure call serving, and then we'll take it from there.
 */
public final class Rpcd {


    /**
     * The set of handlers that are used to serve incoming RPC
     * requests.
     */
    private final Map<String, RpcHandler> handlers =
            new HashMap<String, RpcHandler>();

    /**
     * Register a new handler for a request key.
     * @param key An unique key for handling a request.
     * @param rpcHandler A handler for a request.
     */
    void register(final String key, final RpcHandler rpcHandler) throws RpcdException {
        checkNotNull(rpcHandler);
        checkNotNull(key);
        synchronized (handlers) {

            if (handlers.containsKey(key)) {
                throw new RpcdException("Key already defined: " + key);
            }
            handlers.put(key, rpcHandler);
        }
    }


    /**
     * @param key A key used to identify a handler.
     * @return  True iff a handler is registred for the key.
     */
    public boolean hasHandlerForKey(final String key) {
        checkNotNull(key);
        synchronized (handlers) {
            return handlers.containsKey(key);
        }
    }
}
