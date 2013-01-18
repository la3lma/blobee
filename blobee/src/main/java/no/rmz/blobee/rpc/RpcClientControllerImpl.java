package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public final class RpcClientControllerImpl implements RpcController {

    private boolean failed = false;
    private boolean cancelled = false;
    private String reason = "";
    private final Object monitor = new Object();
    private RpcClient rpcClient;
    private long rpcIndex;

    public RpcClientControllerImpl() {
    }

    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean failed() {
        synchronized (monitor) {
            return failed;
        }
    }

    public String errorText() {
        synchronized (reason) {
            return reason;
        }
    }

    public void startCancel() {
        synchronized (monitor) {
            cancelled = true;
            if (rpcClient != null) {
                rpcClient.cancelInvocation(rpcIndex);
            }
        }
    }

    public void setFailed(final String reason) {
        checkNotNull(reason);
        synchronized (monitor) {
            failed = true;
            this.reason = reason;
        }
    }

    public boolean isCanceled() {
        synchronized (monitor) {
            return cancelled;
        }
    }

    public void notifyOnCancel(RpcCallback<Object> callback) {
        throw new UnsupportedOperationException("notifyOnCancel callback not supported on client side");
    }

    private RpcClientSideInvocation invocation;

    void bindToInvocation(final RpcClientSideInvocation invocation) {
        checkNotNull(invocation);
        synchronized (monitor) {
            if (this.invocation != null) {
                throw new IllegalStateException("invocation was non null");
            }
            this.invocation = invocation;
        }
    }



    void setClientAndIndex(final RpcClient rpcClient, final long rpcIndex) {
        checkNotNull(rpcClient);
        checkArgument(rpcIndex >= 0);
        synchronized (monitor) {
            if (this.rpcClient != null) {
                throw new IllegalArgumentException("rpcClient is already set");
            }
            this.rpcClient = rpcClient;
            this.rpcIndex = rpcIndex;
        }
    }
}
