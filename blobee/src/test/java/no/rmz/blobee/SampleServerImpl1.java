package no.rmz.blobee;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;


public  final class SampleServerImpl1 extends RpcService1 {

    public final static String RETURN_VALUE = "Going home";

    private final RpcResult1 result =
            RpcResult1.newBuilder().setReturnvalue(RETURN_VALUE).build();

    @Override
    public void invoke(
            final RpcController controller,
            final RpcParam1 request,
            final RpcCallback<RpcResult1> done) {
        done.run(result);
    }
}
