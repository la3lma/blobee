package no.rmz.blobee.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

// Stub implementation of the RPC controller.
@Deprecated
public final class RpcControllerImpl implements RpcController {

    public RpcControllerImpl() {
    }



    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean failed() {
        return false;
    }

    public String errorText() {
        return "";
    }

    public void startCancel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setFailed(String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isCanceled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void notifyOnCancel(RpcCallback<Object> callback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
