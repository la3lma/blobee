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
package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.controllers.RpcClientController;
import no.rmz.blobee.controllers.RpcClientControllerImpl;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import no.rmz.blobeeprototest.api.proto.Testservice;
import org.jboss.netty.channel.Channel;


public final class RpcClientImpl implements RpcClient {

    private static final Logger log = Logger.getLogger(RpcClientImpl.class.getName());
    public static final int MAXIMUM_TCP_PORT_NUMBER = 65535;
    private final int capacity;
    final BlockingQueue<RpcClientSideInvocation> incoming;
    private volatile boolean running = false;
    private final Map<Long, RpcClientSideInvocation> invocations =
            new TreeMap<Long, RpcClientSideInvocation>();
    private long nextIndex;
    private Channel channel;

    private final Object mutationMonitor = new Object();
    private final Object runLock = new Object();
    private final static int MAX_CAPACITY_FOR_INPUT_BUFFER = 10000;


    private final MethodSignatureResolver resolver;

    @Override
    public void returnCall(
            final RemoteExecutionContext dc,
            final Message message) {
        synchronized (invocations) {
            final RpcClientSideInvocation invocation =
                    invocations.get(dc.getRpcIndex());
            if (invocation == null) {
                throw new IllegalStateException("Couldn't find call stub for invocation " + dc);
            }
            // Deliver the result then disable the controller
            // so it can be reused.
            final RpcCallback<Message> done = invocation.getDone();
            done.run(message);
            final RpcClientController ctl = invocation.getController();
            invocations.remove(ctl.getIndex());
            invocation.getController().setActive(false);
        }
    }

   private  final Runnable incomingDispatcher = new Runnable() {
        public void run() {
            while (running) {
                sendFirstAvailableOutgoingInvocation();
            }
        }
    };

    private void sendFirstAvailableOutgoingInvocation() {
        try {
            final RpcClientSideInvocation invocation = incoming.take();

            // If the invocation is cancelled already, don't even bother
            // sending it over the wire, just forget about it.
            if (invocation.getController().isCanceled()) {
                return;
            }

            final Long currentIndex = nextIndex++;
            invocations.put(currentIndex, invocation);
            final RpcClientController rcci = (RpcClientController) invocation.getController();

            rcci.setClientAndIndex(this, currentIndex);

            // Then creating the protobuf representation of
            // the invocation, in preparation of sending it
            // down the wire.

            final MethodDescriptor md = invocation.getMethod();
            final String methodName = md.getFullName();
            final String inputType = md.getInputType().getFullName();
            final String outputType = md.getOutputType().getFullName();

            final MethodSignature ms = Rpc.MethodSignature.newBuilder()
                    .setMethodName(methodName)
                    .setInputType(inputType)
                    .setOutputType(outputType)
                    .build();

            final RpcControl invocationControl =
                    Rpc.RpcControl.newBuilder()
                    .setMessageType(Rpc.MessageType.RPC_INVOCATION)
                    .setRpcIndex(currentIndex)
                    .setMethodSignature(ms)
                    .build();

            // Then send the invocation down the wire.
            WireFactory.getWireForChannel(channel)
                    .write(
                    invocationControl,
                    invocation.getRequest());
        }
        catch (InterruptedException ex) {
            log.warning("Something went south");
        }
    }

    public RpcClientImpl(
            final int capacity) {
        this(capacity, new ResolverImpl());
    }

    public RpcClientImpl(final int capacity, final MethodSignatureResolver resolver ) {

        checkArgument(0 < capacity && capacity < MAX_CAPACITY_FOR_INPUT_BUFFER);
        this.capacity = capacity;
        this.incoming =
                new ArrayBlockingQueue<RpcClientSideInvocation>(capacity);
         this.resolver = checkNotNull(resolver);
    }

    public void setChannel(final Channel channel) {
        synchronized (mutationMonitor) {
            if (this.channel != null) {
                throw new IllegalStateException("Can't set channel since channel is already set");
            }
            this.channel = checkNotNull(channel);
        }
    }

    public void start(
            final Channel channel,
            final ChannelShutdownCleaner channelCleanupRunnable) {
        checkNotNull(channel);
        checkNotNull(channelCleanupRunnable);
        synchronized (runLock) {
            setChannel(channel);

            // This runnable will start, then just wait until
            // the channel stops, then the bootstrap will be instructed
            // to reease external resources, and with that the client
            // is completely halted.
            final Runnable channelCleanup = new Runnable() {
                public void run() {
                    // Wait until the connection is closed or the connection attempt fails.
                    channel.getCloseFuture().awaitUninterruptibly();
                    // Then do whatever cleanup is necessary
                    channelCleanupRunnable.shutdownHook();
                }
            };

            running = true;
            final Thread thread = new Thread(channelCleanup, "client shutdown cleaner");
            thread.start();

            final Thread dispatcherThread =
                    new Thread(incomingDispatcher, "Incoming dispatcher for client");
            dispatcherThread.start();
        }
    }

    @Override
    public RpcChannel newClientRpcChannel() {
        return new RpcChannel() {
            public void callMethod(
                    final MethodDescriptor method,
                    final RpcController controller,
                    final Message request,
                    final Message responsePrototype,
                    final RpcCallback<Message> done) {

                checkNotNull(method);
                checkNotNull(controller);
                checkNotNull(request);
                checkNotNull(responsePrototype);
                checkNotNull(done);

                // Binding the controller to this invocation.
                if (controller instanceof RpcClientController) {
                    final RpcClientController ctrl = (RpcClientController) controller;
                    if (ctrl.isActive()) {
                        throw new IllegalStateException("Attempt to activate already active controller");
                    } else {
                        ctrl.setActive(running);
                    }
                }

                final RpcClientSideInvocation invocation =
                        new RpcClientSideInvocation(method, controller, request, responsePrototype, done);
                incoming.add(invocation);
            }
        };
    }

    @Override
    public RpcController newController() {
        return new RpcClientControllerImpl();
    }

    @Override
    public void failInvocation(final long rpcIndex, final String errorMessage) {
        checkNotNull(errorMessage);
        checkArgument(rpcIndex >= 0);
        final RpcClientSideInvocation invocation;
        synchronized (invocations) {
            invocation = invocations.get(rpcIndex);
        }
        checkNotNull(invocation);
        invocation.getController().setFailed(errorMessage);
    }

    @Override
    public void cancelInvocation(final long rpcIndex) {
        checkArgument(rpcIndex >= 0);

        synchronized (invocations) {
            final RpcClientSideInvocation invocation = invocations.get(rpcIndex);
            checkNotNull(invocation);
        }

        final RpcControl cancelRequest =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_CANCEL)
                .setRpcIndex(rpcIndex)
                .build();

        WireFactory.getWireForChannel(channel)
                .write(cancelRequest);
    }

    public RpcClient start() {
        return this;
    }



    public MethodSignatureResolver getResolver() {
        return resolver;
    }


    public RpcClient addProtobuferRpcInterface(final Object instance) {

        if (! (instance instanceof  com.google.protobuf.Service)) {
            throw new IllegalArgumentException("Expected a class extending com.google.protobuf.Service");
        }

        final Service service = (Service) instance;


        final ServiceDescriptor descriptor = service.getDescriptorForType();
        final List<MethodDescriptor> methods = descriptor.getMethods();
        for (final MethodDescriptor md : methods) {
            try {

                final Message inputType = TypeExctractor.getReqestPrototype(service, md);
                final Message outputType = TypeExctractor.getResponsePrototype(service, md);

                // The lines above were made in desperation snce I couldn1t get
                // these two lines to work.  If I made some stupid mistake,
                // and someone can get the two lines below to work, I can
                // remove the entire TypeExtractor class and be very happy
                // about that :-)
                // final MessageLite outputType = md.getOutputType().toProto().getDefaultInstance();
                // final MessageLite inputType = md.getInputType().toProto().getDefaultInstance();

                resolver.addTypes(md, inputType, outputType);
            }

            catch (MethodTypeException ex) {
                /// XXXX Something  more severe should happen here
                Logger.getLogger(RpcClientImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return this;
    }

    public RpcClient addProtobuferRpcInterface(final Class serviceDefinition) {
        checkNotNull(serviceDefinition);

        Method newReflectiveService = null;
        for (final Method m : serviceDefinition.getMethods()) {
            if (m.getName().equals("newReflectiveService")) {
                newReflectiveService = m;
                break;
            }
        }

        if (newReflectiveService == null) {
            throw new IllegalStateException(
                    "class " + serviceDefinition + " is not a service defining class");
        }
        final Object instance;
        try {
            instance = newReflectiveService.invoke(null, (Object) null);
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
        catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }

        addProtobuferRpcInterface(instance);

        return this;
    }


}
