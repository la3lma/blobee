package no.rmz.blobee;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcParam;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcService;

public  final class SampleServerImpl2 extends RpcService {

    private final RpcResult result =
            RpcResult.newBuilder().setReturnvalue(SampleServerImpl1.RETURN_VALUE).build();

    @Override
    public void invoke(
            final RpcController controller,
            final RpcParam request,
            final RpcCallback<RpcResult> done) {
        done.run(result);
    }
}
