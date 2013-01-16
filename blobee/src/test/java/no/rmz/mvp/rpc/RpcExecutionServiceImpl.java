package no.rmz.mvp.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
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

// These two references must be removed for this to become a fully
// abstract service.

import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import org.jboss.netty.channel.ChannelHandlerContext;


// XXX Do whatever is necessary to make this class completely
//     generic, then promote it to the source package
//      directory.

public final class RpcExecutionServiceImpl<
        ServiceType,
        ParamType extends Message,
        ResultType extends Message> implements RpcExecutionService {
    private static final Logger log = Logger.getLogger(RpcExecutionServiceImpl.class.getName());


    private final Class serviceType;
    private final Class paramType;
    private final Class resultType;
    private final ServingRpcChannel servingChannel;
    private final Testservice.RpcService myService;




    public RpcExecutionServiceImpl(final Class serviceType,
            final Class paramType, final Class resultType)
            throws
               NoSuchMethodException,
               IllegalAccessException,
               IllegalArgumentException,
               InvocationTargetException {
        this.serviceType = checkNotNull(serviceType);
        this.paramType = checkNotNull(paramType);
        this.resultType = checkNotNull(resultType);
        final MethodMap methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);
        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
        // XXX Bogus cast
        myService =  Testservice.RpcService.newStub(servingChannel);
    }

    public interface Callable<ParamType, ResultType> {
        public void call(
                final RpcController controller,
                final ParamType requestParam,
                final RpcCallback<ResultType> callback);
    }

    @Override
    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx,
            final Object param) {

        final RpcCallback<Testservice.RpcResult> callback = new RpcCallback<Testservice.RpcResult>() {
            public void run(final RpcResult response) {
                dc.returnResult(response);
            }

        };

         //no.rmz.blobeeprototest.api.proto.Testservice.RpcParam request,
        // com.google.protobuf.RpcCallback<no.rmz.blobeeprototest.api.proto.Testservice.RpcResult> done

        // XXX This is bogus
        final RpcController controller;
        controller = servingChannel.newController();

        final  Testservice.RpcParam  requestParam = (Testservice.RpcParam ) param;
        myService.invoke(controller, requestParam, callback);
    }
}
