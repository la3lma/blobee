/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package no.rmz.blobee.rpc.server;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobee.rpc.client.ResolverImpl;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * The implementation object implements one or more intefaces each of those
 * interfaces present methods that can be served through
 */
public final class RpcExecutionServiceImpl
        implements RpcExecutionService {


    private final static Logger log =
            Logger.getLogger(RpcExecutionServiceImpl.class.getName());

    
    /**
     * A thread pool using the EXCEPTION_HANDLER that is used to
     * execute incoming RPC requests.
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool(
            new ErrorLoggingThreadFactory("Executor thread for RpcExecutionServiceImpl", log));

    private Object implementation;
    private final Map<MethodSignature, Method> mmap;
    private final Map<MethodSignature, Class<?>> returnTypes;
    private final Map<MethodSignature, Class<?>> pmtypes;
    private Map<Class, Object> implementations = new HashMap<Class, Object>();

    private final ControllerStorage controllerStorage = new ControllerStorage();

    public Class getReturnType(final MethodSignature sig) {
        // Preconditions
        checkNotNull(sig);
        checkArgument(!returnTypes.isEmpty());
        final Class<?> result = returnTypes.get(sig);

        // Postcondition
        checkNotNull(result);
        return result;
    }

    public Class getParameterType(final MethodSignature sig) {
        checkNotNull(sig);
        return pmtypes.get(sig);
    }
    final String name;

    public RpcExecutionServiceImpl(final String name) {
        this(name, null);
    }
    private final ExecutionServiceListener listener;

    public RpcExecutionServiceImpl(final String name, ExecutionServiceListener listener) {
        this.name = checkNotNull(name);
        mmap = new HashMap<MethodSignature, Method>();
        returnTypes = new HashMap<MethodSignature, Class<?>>();
        pmtypes = new HashMap<MethodSignature, Class<?>>();
        this.listener = listener;
    }

    @Deprecated
    public RpcExecutionServiceImpl(
            final String name,
            final Object implementation, final Class... interfaceClasses)
            throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            SecurityException,
            IllegalStateException,
            ExecutionServiceException {
        this(name);
        addImplementation(implementation, interfaceClasses);
    }

    public void addImplementation(
            final Object implementation,
            final Class interfaceClasses) throws SecurityException,
            IllegalStateException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException, ExecutionServiceException {

        addImplementation(implementation, new Class[]{interfaceClasses});
    }


    public void addImplementation(
            final Object implementation,
            final Class[] interfaceClasses) throws SecurityException, IllegalStateException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, ExecutionServiceException {
        this.implementation = checkNotNull(implementation);

        final Collection<Class> ifaces = new HashSet<Class>();

        for (final Class i : implementation.getClass().getClasses()) {
            ifaces.add(i);
        }

        for (final Class ic : interfaceClasses) {
            if (implementations.containsKey(ic)) {
                throw new ExecutionServiceException("Interface " + ic + " already has an implementation");
            }
        }

        log.info("The interfaces are " + ifaces);
        for (final Class iface : interfaceClasses) {

            if (!ifaces.contains(iface)) {
                throw new IllegalArgumentException(
                        "The implementation " + implementation + "does not implement interface " + iface);
            }

            for (final Method interfaceMethod : iface.getMethods()) {
                final TypeVariable<Method>[] typeParameters =
                        interfaceMethod.getTypeParameters();

                final String name = interfaceMethod.getName();


                final Descriptors.MethodDescriptor descriptor;
                descriptor = ServiceAnnotationMapper.getMethodDescriptor(
                        implementation.getClass(), name);
                final MethodSignature methodSignature =
                        ResolverImpl.getMethodSignatureFromMethodDescriptor(descriptor);

                final Method implementationMethod = findMethod(name, implementation.getClass());
                if (implementationMethod == null) {
                    throw new IllegalStateException("Unknown method " + name);
                }

                mmap.put(methodSignature, implementationMethod);

                // XXX I need to grok this
                final Class<?>[] parameterTypes = implementationMethod.getParameterTypes();
                final Class pmtype = parameterTypes[1];
                final Type typeOfReturnvalue = extractCallbackParamType(interfaceMethod);

                returnTypes.put(methodSignature, (Class) typeOfReturnvalue);
                pmtypes.put(methodSignature, pmtype);
            }
        }
    }

    private static Type extractCallbackParamType(final Method interfaceMethod) {
        checkNotNull(interfaceMethod);
        // Now calculate the return type:
        // First we get the type of the RpcCallback
        final Type rpcCallbackType = interfaceMethod.getGenericParameterTypes()[2];
        final ParameterizedType ptype = (ParameterizedType) rpcCallbackType;
        final Type[] actualTypeArguments = ptype.getActualTypeArguments();
        final Type typeOfReturnvalue = actualTypeArguments[0];
        return typeOfReturnvalue;
    }

    // XXX Linear search.  I'm sure there is a better way.
    private Method findMethod(final String name, final Class clazz) {
        for (final Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    public void removeController(final ChannelHandlerContext ctx, long rpcIndex) {
         controllerStorage.removeController(ctx, rpcIndex);
    }

    public final static class ControllerCoordinate {

        final ChannelHandlerContext ctx;
        final Long rpcIdx;

        public ControllerCoordinate(ChannelHandlerContext ctx, long rpcIdx) {
            this.ctx = checkNotNull(ctx);
            checkArgument(rpcIdx >= 0);
            this.rpcIdx = rpcIdx;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(rpcIdx, ctx);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ControllerCoordinate) {
                final ControllerCoordinate ob = (ControllerCoordinate) obj;
                return Objects.equal(ctx, ob.ctx)
                        && Objects.equal(rpcIdx, ob.rpcIdx);
            } else {
                return false;
            }
        }
    }

    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx, // XXX Redundant? dc.getCtx or something
            final Object parameter) {



        final Runnable runnable =
                new MethodInvokingRunnable(implementation, dc, ctx, parameter, controllerStorage, this);
        try {
            // For some reason not all the invocations
            // that are submitted are actually run.  The ones that are run
            // does seem to always work correctly though
            // this is a big XXXX
            threadPool.submit(runnable);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Couldn't execute runnable.  That's awful!", e);
        }
    }

    public void startCancel(final ChannelHandlerContext ctx, final long rpcIndex) {
        // XXX Bogus error handling
        controllerStorage.getController(ctx, rpcIndex).startCancel();
        controllerStorage.removeController(ctx, rpcIndex);
    }

    // XXX Why is this public?
    public void listen(
            final ExecutorService threadPool,
            final Object object,
            final Object implementation,
            final Object object0,
            final Object parameter,
            final Object object1) {
        // For debugging.
        if (listener != null) {
            listener.listen(threadPool, null, implementation, null, parameter, null);
        }
    }

    public Method getMethod(final MethodSignature ms) {
        return mmap.get(ms);
    }

    public void storeController(ChannelHandlerContext ctx, long rpcIdx, RpcServiceController controller) {
        controllerStorage.storeController(ctx, rpcIdx, controller);
    }
    // XXX Gjør denne ekstern.
}
