package no.rmz.blobee.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;

public interface RpcClient {

    void cancelInvocation(final long rpcIndex);

    void failInvocation(final long rpcIndex, final String errorMessage);

    RpcChannel newClientRpcChannel();

    RpcController newController();

    void returnCall(final RemoteExecutionContext dc, final Message message);

    RpcClient start();

    RpcClient addProtobuferRpcInterface(final Object instance);

    RpcClient addInterface(final Class serviceInterface);

    MethodSignatureResolver getResolver();
}
