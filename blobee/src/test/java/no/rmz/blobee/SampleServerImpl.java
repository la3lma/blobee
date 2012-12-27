package no.rmz.blobee;

import no.rmz.blobeeproto.api.proto.Rpc;

public final class SampleServerImpl {

    @ProtobufRpcImplementation(
            serviceClass = no.rmz.blobeeproto.api.proto.Rpc.RpcService.class,
            method = "Invoke")
    public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
        return Rpc.RpcResult.newBuilder().setStat(Rpc.StatusCode.HANDLER_FAILURE).build();
    }
}
