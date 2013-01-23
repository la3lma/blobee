/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package no.rmz.blobee.rpc;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobee.controllers.RpcServiceControllerImpl;
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


    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    // XXX No longer final, but the design of this
    //     field is in flux.  May in fact become a
    //     map from interfaces to implementations.
    private  Object implementation;

    private final Map<MethodSignature, Method> mmap;
    private final Map<MethodSignature, Class<?>> returnTypes;
    private final Map<MethodSignature, Class<?>> pmtypes;

    public Class getReturnType(final MethodSignature sig) {
        checkNotNull(sig);
        return returnTypes.get(sig);
    }

     public Class getParameterType(final MethodSignature sig) {
        checkNotNull(sig);
        return pmtypes.get(sig);
    }

     public RpcExecutionServiceImpl() {
        mmap = new HashMap<MethodSignature, Method>();
        returnTypes = new HashMap<MethodSignature, Class<?>>();
        pmtypes = new HashMap<MethodSignature, Class<?>>();
    }

    public RpcExecutionServiceImpl(
            final Object implementation, final Class ... interfaceClasses)
            throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this();
        addImplementation(implementation, interfaceClasses);
    }


    private Map<Class, Object> implementations = new HashMap<Class, Object>();

    private void addImplementation(
            final Object implementation,
            final Class[] interfaceClasses) throws SecurityException, IllegalStateException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException {
        this.implementation = checkNotNull(implementation);

        final Collection<Class> ifaces = new HashSet<Class>();

        for (final Class i: implementation.getClass().getClasses()) {
            ifaces.add(i);
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
            if (obj instanceof  ControllerCoordinate) {
                final ControllerCoordinate ob = (ControllerCoordinate) obj;
             return Objects.equal(ctx, ob.ctx) &&
                    Objects.equal(rpcIdx, ob.rpcIdx);
            } else {
                return false;
            }
        }
    }

    // XXX Use "table" from guava instead?


    public final static class ControllerStorage {

        private final Map<ControllerCoordinate, RpcServiceController> map =
                new HashMap<ControllerCoordinate, RpcServiceController>();

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

    // XXX This is actually a memory leak since nothing ever
    //     gets deleted from this thing.   When an invocation's result
    //     is returned, this structure should be cleaned up.
    private final ControllerStorage controllerStorage = new ControllerStorage();


    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx, // XXX Redundant? dc.getCtx or something
            final Object parameter) {

        // XXX Handle exceptions better!
        final Runnable runnable = new Runnable() {
            public void run() {
                final Method method = mmap.get(dc.getMethodSignature());
                // XXXX Add misc contexts.
                final RpcServiceController controller = new RpcServiceControllerImpl(dc);

                controllerStorage.storeController(ctx, dc.getRpcIndex(), controller);

                final RpcCallback<Message> callback =
                        new RpcCallback<Message>() {
                            public void run(final Message response) {
                                controller.invokeCancelledCallback();
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
        };

        threadPool.submit(runnable);
    }

    public void startCancel(final ChannelHandlerContext ctx, final long rpcIndex) {
        // XXX Bogus error handling
        controllerStorage.getController(ctx, rpcIndex).startCancel();
    }
}
