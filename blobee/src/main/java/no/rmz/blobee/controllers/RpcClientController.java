package no.rmz.blobee.controllers;

import com.google.protobuf.RpcController;
import no.rmz.blobee.rpc.client.RpcClientImpl;
import no.rmz.blobee.rpc.client.RpcClientSideInvocation;


public interface RpcClientController extends RpcController {

    long getIndex();

    boolean isActive();

    void setActive(final boolean active);

    void bindToInvocation(final RpcClientSideInvocation invocation);

    void setClientAndIndex(final RpcClientImpl rpcClient, final long rpcIndex);
}
