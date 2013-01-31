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
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobee.controllers.RpcServiceControllerImpl;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import no.rmz.blobee.rpc.client.ResolverImpl;
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
    private final static UncaughtExceptionHandler EXCEPTION_HANDLER = new UncaughtExceptionHandler() {
        public void uncaughtException(final Thread t, final Throwable e) {
            log.log(Level.SEVERE, "Uncaught exception in thrad " + t, e);
        }
    };
    private final ExecutorService threadPool = Executors.newCachedThreadPool(
            new ThreadFactory() {
                public Thread newThread(Runnable r) {

                    final Thread thread = new Thread(r, "Executor thread for RpcExecutionServiceImpl");
                    thread.setUncaughtExceptionHandler(EXCEPTION_HANDLER);
                    return thread;
                }
            }); // XXX Just a  number
    // XXX No longer final, but the design of this
    //     field is in flux.  May in fact become a
    //     map from interfaces to implementations.
    private Object implementation;
    private final Map<MethodSignature, Method> mmap;
    private final Map<MethodSignature, Class<?>> returnTypes;
    private final Map<MethodSignature, Class<?>> pmtypes;
    private Map<Class, Object> implementations = new HashMap<Class, Object>();
    // XXX This is actually a memory leak since nothing ever
    //     gets deleted from this thing.   When an invocation's result
    //     is returned, this structure should be cleaned up.
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

    // XXXX Refactor this to get the
    //      prototype definitions from the input and output types of a method, and
    //      make those methods public static.  Then use them to implement getPrototypeForClass
    //      in RpcPeerHandler.
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

    // XXX Use "table" from guava instead?
    public final static class ControllerStorage {

        private final Map<ControllerCoordinate, RpcServiceController> map =
                new ConcurrentHashMap<ControllerCoordinate, RpcServiceController>();

        public void storeController(
                final ChannelHandlerContext ctx,
                final long rpcIdx,
                final RpcServiceController controller) {
            checkNotNull(ctx);
            checkArgument(rpcIdx >= 0);
            checkNotNull(controller);
            map.put(new ControllerCoordinate(ctx, rpcIdx), controller);
        }

        public RpcServiceController getController(
                final ChannelHandlerContext ctx,
                final long rpcIdx) {

            final RpcServiceController result =
                    map.get(new ControllerCoordinate(ctx, rpcIdx));
            return result;
        }
    }

    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx, // XXX Redundant? dc.getCtx or something
            final Object parameter) {


        final Runnable runnable = new MethodInvokingRunnable(dc, ctx, parameter);
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
    }

    private class MethodInvokingRunnable implements Runnable {

        private final RemoteExecutionContext dc;
        private final ChannelHandlerContext ctx;
        private final Object parameter;

        public MethodInvokingRunnable(RemoteExecutionContext dc, ChannelHandlerContext ctx, Object parameter) {
            this.dc = dc;
            this.ctx = ctx;
            this.parameter = parameter;
        }

        public void run() {

            final Method method = mmap.get(dc.getMethodSignature());



            final RpcServiceController controller = new RpcServiceControllerImpl(dc);

            // XXX This sometimes fails!
            controllerStorage.storeController(ctx, dc.getRpcIndex(), controller);

            // For debugging.
            if (listener != null) {
                listener.listen(threadPool, null, implementation, null, parameter, null);
            }

            final RpcCallback<Message> callbackAdapter =
                    new RpcCallback<Message>() {
                        public void run(final Message response) {
                            controller.invokeCancelledCallback();
                            dc.returnResult(response);
                        }
                    };


            try {
                method.invoke(implementation, controller, parameter, callbackAdapter);
            }
            // XXX Throwing Runtime Exceptions is evil.
            catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            }
            catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}