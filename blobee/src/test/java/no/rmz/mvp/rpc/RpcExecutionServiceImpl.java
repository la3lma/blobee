package no.rmz.mvp.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.MethodMap;
import no.rmz.blobee.rpc.RemoteExecutionContext;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.ServiceAnnotationMapper;
import no.rmz.blobee.rpc.ServingRpcChannel;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import org.jboss.netty.channel.ChannelHandlerContext;

// XXX Do whatever is necessary to make this class completely
//     generic, then promote it to the source package
//      directory.
public final class RpcExecutionServiceImpl<
        ServiceType, ParamType extends Message, ResultType extends Message> implements RpcExecutionService {

    private static final Logger log = Logger.getLogger(RpcExecutionServiceImpl.class.getName());
    private final Class serviceType;
    private final Class paramType;
    private final Class resultType;
    private final ServingRpcChannel servingChannel;
    private final Testservice.RpcService myService;
    private final Object implementation;
    private final MethodMap methodMap;

    public RpcExecutionServiceImpl(
            final Object implementation,
            final Class serviceType,
            final Class paramType,
            final Class resultType)
            throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        this.serviceType = checkNotNull(serviceType);
        this.paramType = checkNotNull(paramType);
        this.resultType = checkNotNull(resultType);
        this.methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);
        this.implementation = checkNotNull(implementation);
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
        // XXX Bogus cast
        myService = Testservice.RpcService.newStub(servingChannel);
    }

    public MessageLite getReturnTypePrototype(final Class msg) {
        MessageLite prototype = null; // XXX THis is bogus

        try {

            // XXX We should ask the servide to get the response
            //     prototype from the method description.
            final Method method = msg.getMethod("getDefaultInstance");
            try {
                final Object foo;
                try {
                    // foo = msg.newInstance();
                    final Object ob = method.invoke(null);
                    prototype = (MessageLite) ob;
                }

                catch (IllegalAccessException ex) {
                    log.log(Level.SEVERE, "y", ex);
                }

            }
            catch (IllegalArgumentException ex) {
                log.log(Level.SEVERE, "b", ex);
            }
            catch (InvocationTargetException ex) {
                log.log(Level.SEVERE, "c", ex);
            }
        }
        catch (NoSuchMethodException ex) {
            log.log(Level.SEVERE, "d", ex);
        }
        catch (SecurityException ex) {
            log.log(Level.SEVERE, "e", ex);
        }
        return prototype;
    }

    @Override
    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx,
            final Object param) {

        final RpcCallback<Message> callback =
                new RpcCallback<Message>() {
                    public void run(final Message response) {
                        dc.returnResult(response);
                    }
                };


        // XXX This is bogus
        final RpcController controller;
        controller = servingChannel.newController();
        final MethodSignature methodSignature = dc.getMethodSignature();
        final MethodDescriptor methodDescriptor =
                methodMap.getMethodDescriptorFromMethodSignature(methodSignature);
        /*
         *   final MethodDescriptor method,
         final RpcController controller,
         final Message request,
         final Message responsePrototype,
         final RpcCallback<Message> callback) {
         */
        final MessageLite responsePrototype = getReturnTypePrototype(resultType);

        final Testservice.RpcParam requestParam = (Testservice.RpcParam) param;
         servingChannel.callMethod(
         methodDescriptor,
         controller,
         requestParam,
         (Message) responsePrototype,
         callback);
/*
        final Testservice.RpcParam requestParam = (Testservice.RpcParam) param;
        myService.invoke(controller, requestParam, callback);
        */
    }
}
