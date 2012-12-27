package no.rmz.blobee.netty.rpc;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import no.rmz.blobee.netty.echo.*;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RpcServerClientInteractionTest {

    private final static int PORT = 7171;
    private final static String HOST = "localhost";
    private final static int FIRST_MESSAGE_SIZE = 256;




    @Test
    public void testSimpleMessageGoingOneWay() {
        // Uncomment to run forever, watch the blinkenlights  :-)
        // Configure the server.
        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));


        final RpcServerHandler serverHandler = new RpcServerHandler();


        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(serverHandler);
            }
        });

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(PORT));



         // Configure the client.
         ClientBootstrap clientBootstrap = new ClientBootstrap(
                 new NioClientSocketChannelFactory(
                         Executors.newCachedThreadPool(),
                         Executors.newCachedThreadPool()));

         // Set up the pipeline factory.
         clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
             public ChannelPipeline getPipeline() throws Exception {
                 return Channels.pipeline(
                         new RpcClientHandler(FIRST_MESSAGE_SIZE));
             }
         });

         // Check precondition
        assertTrue(serverHandler.getTransferredBytes() ==  0);

         // Start the connection attempt.
         final ChannelFuture future =
                 clientBootstrap.connect(new InetSocketAddress(HOST, PORT));

         // Wait until the connection is closed or the connection attempt fails.
         future.getChannel().getCloseFuture().awaitUninterruptibly();

         // Shut down thread pools to exit.
         clientBootstrap.releaseExternalResources();


         // Then a test
         assertTrue(serverHandler.getTransferredBytes() > 0);
    }
}
