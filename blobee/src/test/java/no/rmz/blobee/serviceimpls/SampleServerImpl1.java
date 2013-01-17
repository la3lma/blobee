package no.rmz.blobee.serviceimpls;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcPar;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcRes;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcServ;


public  final class SampleServerImpl1 extends RpcServ {

    public final static String RETURN_VALUE = "Going home";

    private final RpcRes result =
            RpcRes.newBuilder().setReturnvalue(RETURN_VALUE).build();

    @Override
    public void invoke(
            final RpcController controller,
            final RpcPar request,
            final RpcCallback<RpcRes> done) {
        done.run(result);
    }
}
