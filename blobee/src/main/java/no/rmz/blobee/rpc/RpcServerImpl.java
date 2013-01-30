package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


public final class RpcServerImpl implements RpcServer {
    final InetSocketAddress socket;
    final RpcExecutionService executionService;
    final RpcMessageListener listener;
    final ServerBootstrap bootstrap;

    public RpcServerImpl(final InetSocketAddress socket, final RpcExecutionService executionService, final RpcMessageListener listener) {
        this.socket = checkNotNull(socket);
        this.executionService = checkNotNull(executionService);
        this.listener = checkNotNull(listener);
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
}
