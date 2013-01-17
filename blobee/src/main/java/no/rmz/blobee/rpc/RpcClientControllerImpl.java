package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public final class RpcClientControllerImpl implements RpcController {

    private boolean failed = false;
    private String reason = "";
    private final Object monitor = new Object();

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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setFailed(final String reason) {
        checkNotNull(reason);
        synchronized (monitor) {
            failed = true;
            this.reason = reason;
        }
    }

    public boolean isCanceled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void notifyOnCancel(RpcCallback<Object> callback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
