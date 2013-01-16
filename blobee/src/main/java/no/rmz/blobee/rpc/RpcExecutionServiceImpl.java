package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;


public final class RpcExecutionServiceImpl implements RpcExecutionService {

    private static final Logger log = Logger.getLogger(RpcExecutionServiceImpl.class.getName());
    private final ServingRpcChannel servingChannel;
    private final Object implementation;
    private final MethodMap methodMap;

    public RpcExecutionServiceImpl(
            final Object implementation)
            throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {

        this.methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);
        this.implementation = checkNotNull(implementation);
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
    }

    private static MessageLite getReturnTypePrototype(final Class returnType)
            throws RpcExecutionException {

        try {
            final Method method = returnType.getMethod("getDefaultInstance");
            try {
                try {
                    final Object ob = method.invoke(null);
                    return (MessageLite) ob;
                }
                catch (IllegalAccessException ex) {
                    throw new RpcExecutionException(ex);
                }
            }
            catch (IllegalArgumentException ex) {
                throw new RpcExecutionException(ex);
            }
            catch (InvocationTargetException ex) {
                throw new RpcExecutionException(ex);
            }
        }
        catch (NoSuchMethodException ex) {
            throw new RpcExecutionException(ex);
        }
        catch (SecurityException ex) {
            throw new RpcExecutionException(ex);
        }
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

        final RpcController controller;
        controller = servingChannel.newController();
        final MethodSignature methodSignature = dc.getMethodSignature();
        final MethodDescriptor methodDescriptor =
                methodMap.getMethodDescriptorFromMethodSignature(methodSignature);

        final DescriptorProto responsePrototype =
                methodDescriptor.getOutputType().toProto().getDefaultInstance();

        servingChannel.callMethod(
                methodDescriptor,
                controller,
                (Message) param,
                (Message) responsePrototype,
                callback);
    }
}
