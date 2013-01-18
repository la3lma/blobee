package no.rmz.blobee.controllers;

import com.google.protobuf.RpcController;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcClientSideInvocation;


public interface RpcClientController extends RpcController {

    long getIndex();

    boolean isActive();

    void setActive(final boolean active);

    void bindToInvocation(final RpcClientSideInvocation invocation);

    void setClientAndIndex(final RpcClient rpcClient, final long rpcIndex);
}
