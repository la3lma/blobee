package no.rmz.mvp.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.MethodMap;
import no.rmz.blobee.rpc.RemoteExecutionContext;
import no.rmz.blobee.rpc.RpcControllerImpl;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.ServiceAnnotationMapper;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;


/**
 * The implementation object implements one or more intefaces
 * each of those interfaces present methods that can be
 * served through
 */
public final class ImplementingInterfacesRpcExecutionServiceImpl
    implements RpcExecutionService {


    private final static Logger log =
            Logger.getLogger(ImplementingInterfacesRpcExecutionServiceImpl.class.getName());

    final Object implementation;

    final Map<MethodSignature, Method> mmap;

    public ImplementingInterfacesRpcExecutionServiceImpl(
            final Object implementation, final Class ... interfaces) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.implementation = checkNotNull(implementation);
        mmap = new HashMap<MethodSignature, Method>();

        for (final Class iface : interfaces) {
            for (final Method interfaceMethod : iface.getMethods()) {
                final TypeVariable<Method>[] typeParameters =
                        interfaceMethod.getTypeParameters();
                // final Class<?> returnType = interfaceMethod.getReturnType();
                final String name = interfaceMethod.getName();
                // final String className = iface.getClass().getName();

                final Descriptors.MethodDescriptor descriptor;
                descriptor = ServiceAnnotationMapper.getMethodDescriptor(
                        implementation.getClass(),
                        name.substring(0, 1).toUpperCase()+ // XXX FUCKING UGLY
                        name.substring(1)
                        );
                final MethodSignature methodSignature =
                        MethodMap.getMethodSignatureFromMethodDescriptor(descriptor);

                boolean foundMethod = false;
                for (final Method implementationMethod: implementation.getClass().getMethods()) {
                    if (implementationMethod.getName().equals(name)) {
                         mmap.put(methodSignature, implementationMethod);
                         foundMethod = true;
                         break;
                    }
                }
                if (!foundMethod) {
                    throw new IllegalStateException("Unknown method " + name);
                }
            }
        }
    }

    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx, // XXX Redundant? dc.getCtx or something
            final Object parameter) {
        final Method method = mmap.get(dc.getMethodSignature());

        final RpcController controller= new RpcControllerImpl(); // XX Placeholder

        final RpcCallback<Message> callback =
                new RpcCallback<Message>() {
                    public void run(final Message response) {
                        dc.returnResult(response);
                    }
                };
        try {
            method.invoke(implementation, controller, parameter, callback);
        }
        catch (IllegalAccessException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        catch (IllegalArgumentException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        catch (InvocationTargetException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}
