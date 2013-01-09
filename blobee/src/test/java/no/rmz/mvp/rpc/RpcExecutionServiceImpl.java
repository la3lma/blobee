package no.rmz.mvp.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import no.rmz.blobee.SampleServerImpl;
import no.rmz.blobee.rpc.MethodMap;
import no.rmz.blobee.rpc.RemoteExecutionContext;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.ServiceAnnotationMapper;
import no.rmz.blobee.rpc.ServingRpcChannel;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeprototest.api.proto.Testservice;
import org.jboss.netty.channel.ChannelHandlerContext;


// XXX Do whatever is necessary to make this class completely
//     generic, then promote it to the source package
//      directory.

public final class RpcExecutionServiceImpl implements RpcExecutionService {
    private static final Logger log = Logger.getLogger(RpcExecutionServiceImpl.class.getName());


    final ServingRpcChannel servingChannel;
    final Testservice.RpcService myService;

    public RpcExecutionServiceImpl() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final MethodMap methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);
        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
        myService = Testservice.RpcService.newStub(servingChannel);
    }

    @Override
    public void execute(final RemoteExecutionContext dc, final ChannelHandlerContext ctx, final Object param) {

        final RpcCallback<Testservice.RpcResult> callback = new RpcCallback<Testservice.RpcResult>() {
            public void run(final Testservice.RpcResult response) {
                dc.returnResult(response);
            }
        };
        RpcController servingController;
        servingController = servingChannel.newController();
        final  Testservice.RpcParam request = (Testservice.RpcParam) param;
        myService.invoke(servingController, request, callback);
    }
}
