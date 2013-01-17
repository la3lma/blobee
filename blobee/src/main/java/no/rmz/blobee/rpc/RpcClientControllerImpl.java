package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public final class RpcClientControllerImpl implements RpcController {

    private boolean failed = false;
    private boolean cancelled = false;
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
        synchronized (monitor) {
            cancelled = true;
        }
        // The cancellation needs to be sent to the
        // message scheduler and/or ove the wire to the
        // running service.
        throw new UnsupportedOperationException("Cancellation not fully supported yet");
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
}
