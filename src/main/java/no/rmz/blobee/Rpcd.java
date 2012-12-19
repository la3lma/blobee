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
            new HashMap<String, RpcHandler>(); // XXX Shoud be synced

    /**
     * Register a new handler for a request key.
     * @param key An unique key for handling a request.
     * @param rpcHandler A handler for a request.
     */
    public void register(final String key, final RpcHandler rpcHandler)
            throws RpcdException {

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

    void invoke(
            final String key,
            final RpcParam param,
            final RpcResultHandler rpcResultHandler) {

        checkNotNull(key);
        checkNotNull(param);
        checkNotNull(rpcResultHandler);

        synchronized (handlers) {
            final RpcHandler handler = handlers.get(key);
            if (handler == null) {
                rpcResultHandler.receiveResult(RpcResult.NO_HANDLER);
            } else {
                final RpcResult result = handler.invoke(param);
                if (result == null) {
                    rpcResultHandler.receiveResult(RpcResult.HANDLER_FAILURE);
                } else {
                    rpcResultHandler.receiveResult(result);
                }
            }
        }
    }
}
