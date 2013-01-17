package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobeeproto.api.proto.Rpc.MessageType;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;

public final class RpcServiceControllerImpl implements RpcController {

    final RemoteExecutionContext executionContext;


    RpcServiceControllerImpl(final RemoteExecutionContext dc) {
        this.executionContext = checkNotNull(dc);
    }

    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    boolean failed = false;
    public boolean failed() {
        return failed;
    }

    public String errorText() {
       throw new UnsupportedOperationException("Not supported in server side RpcController");
    }

    public void startCancel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setFailed(final String reason) {
        checkNotNull(reason);
        failed = true;
        final RpcControl msg = RpcControl.newBuilder()
                .setMessageType(MessageType.INVOCATION_FAILED)
                .setRpcIndex(executionContext.getRpcIndex())
                .setFailed(reason)
                .build();

        executionContext.sendControlMessage(msg);
    }

    public boolean isCanceled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void notifyOnCancel(final RpcCallback<Object> callback) {
        checkNotNull(callback);
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
