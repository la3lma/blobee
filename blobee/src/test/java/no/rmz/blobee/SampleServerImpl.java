package no.rmz.blobee;

import no.rmz.blobeeproto.api.proto.Rpc;


public final class SampleServerImpl {

    public final static String RETURN_VALUE = "Going home";

    @ProtobufRpcImplementation(
            serviceClass = no.rmz.blobeeproto.api.proto.Rpc.RpcService.class,
            method = "Invoke")
    public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
        return Rpc.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();
    }
}
