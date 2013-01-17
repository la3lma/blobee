package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public final class RpcClient {

    private static final Logger log = Logger.getLogger(RpcClient.class.getName());
    public static final int MAXIMUM_TCP_PORT_NUMBER = 65535;
    private final int capacity;
    final BlockingQueue<RpcClientSideInvocation> incoming;
    private volatile boolean running = false;
    private final Map<Long, RpcClientSideInvocation> invocations =
            new TreeMap<Long, RpcClientSideInvocation>();
    private final int port;
    private final String host;
    private long nextIndex;
    private Channel channel;
    private RpcPeerPipelineFactory clientPipelineFactory;
    private final Object mutationMonitor = new Object();
    private final Object runLock = new Object();
    private ClientBootstrap clientBootstrap;
    private final static int MAX_CAPACITY_FOR_INPUT_BUFFER = 10000;

    void returnCall(final RemoteExecutionContext dc, final Message message) {
        synchronized (invocations) {
            final RpcClientSideInvocation invocation =
                    invocations.get(dc.getRpcIndex());
            if (invocation == null) {
                throw new IllegalStateException("Couldn't find call stub for invocation " + dc);
            }

            invocation.getDone().run(message);
        }
    }

    final Runnable incomingDispatcher = new Runnable() {
        public void run() {
            while (running) {
                sendFirstAvailableOutgoingInvocation();
            }
        }
    };

    private void sendFirstAvailableOutgoingInvocation() {
        try {
            // down the wire.
            // get back to it later
            final RpcClientSideInvocation invocation = incoming.take();
            final Long currentIndex = nextIndex++;
            invocations.put(currentIndex, invocation);

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


    public RpcClient(
            final int capacity,
            final String host,
            final int port) {

        checkArgument(0 < capacity && capacity < MAX_CAPACITY_FOR_INPUT_BUFFER);
        this.capacity = capacity;
        this.incoming =
                new ArrayBlockingQueue<RpcClientSideInvocation>(capacity);
        checkArgument(0 < port &&  port < MAXIMUM_TCP_PORT_NUMBER);
        this.port = port;
        this.host = checkNotNull(host);
    }

    public void setChannel(final Channel channel) {
        synchronized (mutationMonitor) {
            if (this.channel != null) {
                throw new IllegalStateException("Can't set channel since channel is already set");
            }
            this.channel = checkNotNull(channel);
        }
    }

    public void setClientPipelineFactory(final RpcPeerPipelineFactory pf) {
        synchronized (mutationMonitor) {
            // XXX Synchronization missing
            if (clientPipelineFactory != null) {
                throw new IllegalStateException("Can't set clientPipelineFactory already set");
            }

            this.clientPipelineFactory = checkNotNull(pf);
        }
    }

    void setBootstrap(final ClientBootstrap clientBootstrap) {
        if (this.clientBootstrap != null) {
            throw new IllegalStateException("Can't set clientBotstrap more than once");
        }
        this.clientBootstrap = checkNotNull(clientBootstrap);
    }

    public void start() {

        synchronized (runLock) {
            if (running) {
                throw new IllegalStateException("Cannot start an already running RPC Client");
            }
            running = true;
        }

        // Start the connection attempt.
        final ChannelFuture future =
                clientBootstrap.connect(new InetSocketAddress(host, port));

        setChannel(future.getChannel());

        final Runnable runnable = new Runnable() {
            public void run() {
                // Wait until the connection is closed or the connection attempt fails.
                future.getChannel().getCloseFuture().awaitUninterruptibly();

                // Shut down thread pools to exit.
                clientBootstrap.releaseExternalResources();
            }
        };

        final Thread thread = new Thread(runnable, "client cleaner");
        thread.start();

        final Thread dispatcherThread =
                new Thread(incomingDispatcher, "Incoming dispatcher");
        dispatcherThread.start();
    }

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

                final RpcClientSideInvocation invocation =
                        new RpcClientSideInvocation(method, controller, request, responsePrototype, done);
                incoming.add(invocation);
            }
        };
    }

    public RpcController newController(final RpcChannel rchannel) {
        return new RpcClientControllerImpl();
    }

    void failInvocation(final long rpcIndex, final String errorMessage) {
        checkNotNull(errorMessage);
        checkArgument(rpcIndex >= 0);
        final RpcClientSideInvocation invocation;
        synchronized (invocations) {
            invocation = invocations.get(rpcIndex);
        }
        checkNotNull(invocation);
        invocation.getController().setFailed(errorMessage);
    }
}
