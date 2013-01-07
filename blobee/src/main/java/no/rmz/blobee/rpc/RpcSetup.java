package no.rmz.blobee.rpc;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class RpcSetup {

    /**
     * Utility class, no public constructor for you!
     */
    private RpcSetup() {
    }

    public static void setUpServer(
            final int port, RpcExecutionService executionService, RpcClient rpcClient) {
        setUpServer(port, executionService, null);
    }

    public static void setUpServer(
            final int port,
            final RpcExecutionService executionService,
            final RpcClient rpcClient,
            final RpcMessageListener listener) {

        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        final String name = "server at port " + port;

        final RpcPeerPipelineFactory serverChannelPipelineFactory =
                new RpcPeerPipelineFactory(
                "server accepting incoming connections at port ", executionService, rpcClient, listener);

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);


        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));

    }

    public static RpcClient setUpClient(
            final String host,
            final int port,
            final RpcExecutionService executor) {
        return setUpClient(host, port, executor, null);
    }

    public static RpcClient setUpClient(
            final String host,
            final int port,
            final RpcExecutionService executor,
            final RpcMessageListener listener) {

        // Configure the client.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        final int bufferSize = 1;
        final RpcClient rpcClient = new RpcClient(bufferSize);

        final String name =
                "client connected to server at host " + host + " port " + port;
        final RpcPeerPipelineFactory clientPipelineFactory =
                new RpcPeerPipelineFactory(name, executor,  rpcClient, listener);
        rpcClient.setClientPipelineFactory(clientPipelineFactory);

        clientBootstrap.setPipelineFactory(
                clientPipelineFactory);

        // Start the connection attempt.
        final ChannelFuture future =
                clientBootstrap.connect(new InetSocketAddress(host, port));

        rpcClient.setChannel(future.getChannel());

        rpcClient.start();

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

        return rpcClient;
    }
}
