package no.rmz.blobee.rpc;

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
            MethodDescriptor method,
            RpcController controller,
            Message request,
            Message responsePrototype,
            RpcCallback<Message> done) {
        // XXX Checknotnulls
        this.method = method;
        this.controller = controller;
        this.request = request;
        this.responsePrototype = responsePrototype;
        this.done = done;
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
