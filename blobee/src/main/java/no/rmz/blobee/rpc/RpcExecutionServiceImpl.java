package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;


/**
 * The implementation object implements one or more intefaces
 * each of those interfaces present methods that can be
 * served through
 */
public final class RpcExecutionServiceImpl
    implements RpcExecutionService {


    private final static Logger log =
            Logger.getLogger(RpcExecutionServiceImpl.class.getName());

    final Object implementation;

    final Map<MethodSignature, Method> mmap;
    final Map<MethodSignature, Class<?>> returnTypes;
    final Map<MethodSignature, Class<?>> pmtypes;

    public Class getReturnType(final MethodSignature sig) {
        checkNotNull(sig);
        return returnTypes.get(sig);
    }

     public Class getParameterType(final MethodSignature sig) {
        checkNotNull(sig);
        return pmtypes.get(sig);
    }

    public RpcExecutionServiceImpl(
            final Object implementation, final Class ... interfaces) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.implementation = checkNotNull(implementation);
        mmap = new HashMap<MethodSignature, Method>();
        returnTypes = new HashMap<MethodSignature, Class<?>>();
        pmtypes = new HashMap<MethodSignature, Class<?>>();

        final Collection<Class> ifaces = new HashSet<Class>();

        for (final Class i: implementation.getClass().getInterfaces()) {
            ifaces.add(i);
        }

        for (final Class iface : interfaces) {
/*
            if (!ifaces.contains(iface)) {
                throw new IllegalArgumentException(
                        "The implementation " + implementation + "does not implement interface " + iface);
            }
*/
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

                         final Class<?>[] parameterTypes = implementationMethod.getParameterTypes();
                         final Class pmtype = parameterTypes[1];

                         // Now calculate the return type:
                         // First we get the type of the RpcCallback
                         final Type rpcCallbackType = interfaceMethod.getGenericParameterTypes()[2];
                         ParameterizedType ptype = (ParameterizedType)rpcCallbackType;
                         Type[] actualTypeArguments = ptype.getActualTypeArguments();
                         Type typeOfReturnvalue = actualTypeArguments[0];

                         returnTypes.put(methodSignature, (Class)typeOfReturnvalue);
                         pmtypes.put(methodSignature, pmtype);
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

        // XXX Swalloing exceptions is baaad, but for now that's what
        //     we're doing.
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
