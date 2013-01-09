package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;


public final class RpcClientSideInvocation {
    private final MethodDescriptor method;
    private final RpcController controller;
    private final Message request;
    private final Message responsePrototype;
    private final RpcCallback<Message> done;

    public RpcClientSideInvocation(
            final MethodDescriptor method,
            final RpcController controller,
            final Message request,
            final Message responsePrototype,
            final RpcCallback<Message> done) {
        
        this.method = checkNotNull(method);
        this.controller = checkNotNull(controller);
        this.request = checkNotNull(request);
        this.responsePrototype = checkNotNull(responsePrototype);
        this.done = checkNotNull(done);
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public RpcController getController() {
        return controller;
    }

    public Message getRequest() {
        return request;
    }

    public Message getResponsePrototype() {
        return responsePrototype;
    }

    public RpcCallback<Message> getDone() {
        return done;
    }
}
