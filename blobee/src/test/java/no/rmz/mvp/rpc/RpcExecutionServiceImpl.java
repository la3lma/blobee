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
import org.jboss.netty.channel.ChannelHandlerContext;

public final class RpcExecutionServiceImpl implements RpcExecutionService {
    private static final Logger log = Logger.getLogger(RpcExecutionServiceImpl.class.getName());
    private Rpc.RpcParam request;
    final ServingRpcChannel servingChannel;
    final Rpc.RpcService myService;

    public RpcExecutionServiceImpl() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final MethodMap methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);
        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
        request = Rpc.RpcParam.newBuilder().build();
        myService = Rpc.RpcService.newStub(servingChannel);
    }

    @Override
    public void execute(final RemoteExecutionContext dc, final ChannelHandlerContext ctx, final Object param) {
        log.info("Executing dc = " + dc + ", param = " + param);
        final RpcCallback<Rpc.RpcResult> callback = new RpcCallback<Rpc.RpcResult>() {
            public void run(final Rpc.RpcResult response) {
                dc.returnResult(response);
            }
        };
        RpcController servingController;
        servingController = servingChannel.newController();
        myService.invoke(servingController, request, callback);
    }

}
