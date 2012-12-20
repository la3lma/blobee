package no.rmz.blobee;

import static com.google.common.base.Preconditions.checkNotNull;

public class RpcResult {

    public final static RpcResult NO_HANDLER =
            new RpcResult(RpcStatusCode.NO_HANDLER);
    public final static RpcResult HANDLER_FAILURE =
            new RpcResult(RpcStatusCode.HANDLER_FAILURE);

    private final RpcStatusCode statusCode;

    public RpcResult(final RpcStatusCode rpcStatusCode) {
        this.statusCode = checkNotNull(rpcStatusCode);
    }
}
