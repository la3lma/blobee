package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Service;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import no.rmz.blobeeprototest.api.proto.Testservice;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class RpcServerImpl implements RpcServer {

    final InetSocketAddress socket;
    final RpcExecutionService executionService;
    final RpcMessageListener listener;
    final ServerBootstrap bootstrap;

    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcMessageListener listener) {
        this(socket,
                new RpcExecutionServiceImpl("Execution service for server listening on " + socket.toString()),
                listener);
    }

     public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcMessageListener listener,
            final ExecutionServiceListener esListener) {
        this(socket,
                new RpcExecutionServiceImpl(
                    "Execution service for server listening on " + socket.toString(),
                    esListener),
                listener);
    }

    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcExecutionService executionService,
            final RpcMessageListener listener) {
        this.socket = checkNotNull(socket);
        this.executionService = checkNotNull(executionService);
        this.listener = listener; // XXX Nullable
        this.bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        final String name = "RPC Server at " + socket.toString();
        final RpcPeerPipelineFactory serverChannelPipelineFactory = new RpcPeerPipelineFactory(name, executionService, new MultiChannelClientFactory(), listener);
        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);
    }

    public RpcServer start() {
        // Bind and start to accept incoming connections.
        bootstrap.bind(socket);
        return this;
    }

    /**
     * Add a service to the server.
     *
     * @param service
     * @param iface
     * @return
     */
    public RpcServer addImplementation(final Service service, final Class iface) {
        checkNotNull(service);
        checkNotNull(iface);
        try {
            executionService.addImplementation(service, iface);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex); // XXX
        }
        return this;
    }
}
