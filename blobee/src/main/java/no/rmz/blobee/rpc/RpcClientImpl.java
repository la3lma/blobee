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
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import java.net.InetSocketAddress;
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
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

// XXX TODO   Separate out the parts that
//            handles the connects, bootstraps etc.
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

        checkArgument(0 < capacity && capacity < MAX_CAPACITY_FOR_INPUT_BUFFER);
        this.capacity = capacity;
        this.incoming =
                new ArrayBlockingQueue<RpcClientSideInvocation>(capacity);
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

    public void start() {
        throw new UnsupportedOperationException("Not supported yet.");
    }



    public MethodSignatureResolver getResolver() {
        return mm;
    }

    // XXX This is a hack must be generalized.
    private final MethodMap mm = new MethodMap();

    public void addProtobuferRpcInterface(final Object instance) {

            // final Class pbufAbstractClass) {

        /*
        checkNotNull(pbufAbstractClass);
        final Object instance;
        try {
            instance = pbufAbstractClass.newInstance();
        }

        catch (InstantiationException ex) {
            throw new IllegalArgumentException("Expected a class extending com.google.protobuf.Service", ex);
        }
        catch (IllegalAccessException ex) {
           throw new IllegalArgumentException("Expected a class extending com.google.protobuf.Service", ex);
        }  * */
        if (! (instance instanceof  com.google.protobuf.Service)) {
            throw new IllegalArgumentException("Expected a class extending com.google.protobuf.Service");
        }


/*
        com.google.protobuf.Service service = (com.google.protobuf.Service) instance;
        ServiceDescriptor descriptor = service.getDescriptorForType();
        final List<MethodDescriptor> methods = descriptor.getMethods();
*/
        /// I'm jst trying to shortcut the generics to ensure that we
        //  are upcasting to the most specific type we can to see if that
        //  helps when getting the input/output types, but it seems that
        //  is not happening.  What should I do?
        final Testservice.RpcService service = (Testservice.RpcService) instance;


        final ServiceDescriptor descriptor = service.getDescriptorForType();
        final List<MethodDescriptor> methods = descriptor.getMethods();
        for (final MethodDescriptor md : methods) {
            try {
                final String fullName = md.getFullName();
                // XXX Why do I only get completely useless types from these
                //     two queries.  The results are in no way useful for
                //     deserialization.  It seeems I just can't get the proper
                //     types out of the Google code.  But why?
                final Message inputType = TypeExctractor.getReqestPrototype(service, md);
                final Message outputType = TypeExctractor.getResponsePrototype(service, md);
                // final MessageLite outputType = md.getOutputType().toProto().getDefaultInstance();
                // final MessageLite outputType = md.getOutputType().toProto().getDefaultInstance();

                mm.addTypes(md, inputType, outputType);
            }
            // use cpbufAbstractClass.getResponsePrototype , just need to have
            //     the method indexes and that is probably also something we can
            //     extract from the class.
            // XXX Traverse the pbufAbstractClass looking for
            //     abstract methods, they will be implementing
            //     the RPC interface, and that is what we need to implement
            //     and map types for.  Extract types and add it to a
            //     structure that can be used for type lookup.
            catch (MethodTypeException ex) {
                /// XXXX Something  more severe should happen here
                Logger.getLogger(RpcClientImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }






        // use cpbufAbstractClass.getResponsePrototype , just need to have
        //     the method indexes and that is probably also something we can
        //     extract from the class.

        // XXX Traverse the pbufAbstractClass looking for
        //     abstract methods, they will be implementing
        //     the RPC interface, and that is what we need to implement
        //     and map types for.  Extract types and add it to a
        //     structure that can be used for type lookup.
    }


}
