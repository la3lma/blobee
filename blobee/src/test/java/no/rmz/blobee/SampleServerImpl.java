package no.rmz.blobee;

import no.rmz.blobee.rpc.ProtobufRpcImplementation;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcService;


public final class SampleServerImpl {

    public final static String RETURN_VALUE = "Going home";

    @ProtobufRpcImplementation(
            serviceClass =  RpcService.class,
            method = "Invoke")
    public Testservice.RpcResult invoke(final Testservice.RpcParam param) {
        return Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();
    }
}
