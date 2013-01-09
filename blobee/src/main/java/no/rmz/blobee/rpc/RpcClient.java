package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
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
    private final int capacity;
    final BlockingQueue<RpcClientSideInvocation> incoming;
    private volatile boolean running = true;
    private final Map<Long, RpcClientSideInvocation> invocations =
            new TreeMap<Long, RpcClientSideInvocation>();
    private final int port;
    private final String host;
    private long nextIndex;
    private Channel channel;
    private RpcPeerPipelineFactory clientPipelineFactory;
    private final Object channelMonitor = new Object();
    private ClientBootstrap clientBootstrap;

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
                try {
                    // First we remember this invocation so we can
                    // get back to it later
                    final RpcClientSideInvocation invocation = incoming.take();
                    final Long currentIndex = nextIndex++;
                    invocations.put(currentIndex, invocation);

                    // Then creating the protobuf representation of
                    // the invocation, in preparation of sending it
                    // down the wire.

                    final MethodDescriptor md = invocation.getMethod();
                    final String methodName = md.getFullName();
                    final String inputType = md.getInputType().getName();
                    final String outputType = md.getOutputType().getName();

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
                    // XXX This is a bug! Needs to be -strictly- serialized
                    //     so stronger serialization is required
                    //     on this operation!
                    // Perhaps use getChannelLock in RpcPeerHandler
                    WireFactory.getWireForChannel(channel)
                            .write(
                                invocationControl,
                                invocation.getRequest());
                }
                catch (InterruptedException ex) {
                    log.warning("Something went south");
                }
            }
        }
    };

    public RpcClient(
            final int capacity,
            String host,
            int port) {
        // XXX Checking of params
        this.capacity = capacity;
        this.incoming =
                new ArrayBlockingQueue<RpcClientSideInvocation>(capacity);
        this.port = port;
        this.host = host;
    }

    public void setChannel(final Channel channel) {
        synchronized (channelMonitor) {
            // XXX Synchronization missing
            if (this.channel != null) {
                throw new IllegalStateException("Can't set channel since channel is already set");
            }

            this.channel = channel;
        }
    }

    public void setClientPipelineFactory(RpcPeerPipelineFactory pf) {
        // XXX Synchronization missing
        if (clientPipelineFactory != null) {
            throw new IllegalStateException("Can't set clientPipelineFactory already set");
        }

        this.clientPipelineFactory = pf;
    }

    // XXX Allow only start once, make thread safe
    public void start() {

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
        return new RpcControllerImpl();
    }

    void setBootstrap(final ClientBootstrap clientBootstrap) {
        this.clientBootstrap = checkNotNull(clientBootstrap);
    }
}
