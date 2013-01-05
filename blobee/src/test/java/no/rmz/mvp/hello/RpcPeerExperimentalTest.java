package no.rmz.mvp.hello;

import com.google.protobuf.MessageLite;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.WeakHashMap;
import javax.xml.ws.handler.MessageContext;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * An essential test: Testing that we can send two completely different types of
 * things over the same netty channel.
 */
@RunWith(MockitoJUnitRunner.class)
public final class RpcPeerExperimentalTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.mvp.hello.RpcPeerExperimentalTest.class.getName());
    private final static String PARAMETER_STRING = "Hello server";
    private final static int PORT = 7172;
    private final static String HOST = "localhost";
    private final static int FIRST_MESSAGE_SIZE = 256;

    // We need an interface to receive something into a mock
    // and this is it.
    public interface Receiver<T> {

        public void receive(final T param);
    }
    // This is the receptacle for the message that goes
    // over the wire.
    @Mock
    Receiver<Rpc.RpcParam> serverParamReceiver;
    @Mock
    Receiver<Rpc.RpcControl> serverControlReceiver;
    private Rpc.RpcParam sampleRpcMessage;
    private Rpc.RpcControl sampleControlMessage;
    private Rpc.RpcControl heartbeatMessage;

    private RpcPeerPipelineFactory clientPipelineFactory;

    @Before
    public void setUp() {

        heartbeatMessage =
                Rpc.RpcControl.newBuilder().setType(Rpc.MessageType.HEARTBEAT).build();


        clientPipelineFactory = new RpcPeerPipelineFactory("server");
    }


    public interface RpcMessageListener {
        public void receiveMessage(Object message, ChannelHandlerContext ctx);
    }

    public static final class RpcPeerHandler extends SimpleChannelUpstreamHandler {
        private final static Rpc.RpcControl HEARTBEAT =
                Rpc.RpcControl.newBuilder().setType(Rpc.MessageType.HEARTBEAT).build();

        private final DynamicProtobufDecoder protbufDecoder;

        /**
         * Used to listen in to incoming messages. Intended for
         * debugging purposes.
         */
        private RpcMessageListener listener;

        private final Object listenerLock = new Object();

        private RpcPeerHandler(final DynamicProtobufDecoder protbufDecoder) {
            this.protbufDecoder = checkNotNull(protbufDecoder);
        }

        public void setListener(RpcMessageListener listener) {
            synchronized (listenerLock) {
                this.listener = listener;
            }
        }



        @Override
        public void channelConnected(
                final ChannelHandlerContext ctx, final ChannelStateEvent e) {
            e.getChannel().write(HEARTBEAT);
        }

        @Override
        public void messageReceived(
                final ChannelHandlerContext ctx,
                final MessageEvent e) {
            final Object message = e.getMessage();

            // First send the object to the listener, if we have one.
            synchronized (listenerLock) {
                if (listener != null) {
                    listener.receiveMessage(message, ctx);
                }
            }

            // Then parse it the regular way.
            if (message instanceof Rpc.RpcControl) {
                final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();

                protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            } else {
                fail("Unknown type of incoming message to server: " + message);
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            // Close the connection when an exception is raised.
            log.log(
                    Level.WARNING,
                    "Unexpected exception from downstream.",
                    e.getCause());
            e.getChannel().close();
        }
    }


    public final class RpcPeerPipelineFactory implements ChannelPipelineFactory {

        private final String name;
        private final WeakHashMap<ChannelPipeline, DynamicProtobufDecoder> decoderMap =
                new WeakHashMap<ChannelPipeline, DynamicProtobufDecoder>();

        private RpcMessageListener listener;

        public RpcPeerPipelineFactory(final String name) {
            this.name = checkNotNull(name);
        }

        // XXX for testing
        protected RpcPeerPipelineFactory(final String name, final RpcMessageListener listener) {
            this.name = checkNotNull(name);
            this.listener = checkNotNull(listener);
        }

        public void putNextPrototype(final ChannelPipeline pipeline, final MessageLite prototype) {
            if (decoderMap.containsKey(pipeline)) {
                decoderMap.get(pipeline).putNextPrototype(prototype.getDefaultInstanceForType());
            } else {
                throw new RuntimeException("This is awful");
            }
        }

        // XXX Eventually this thing should get things like
        //     compression, ssl, http, whatever, but for not it's just
        //     the simplest possible pipeline I could get away with, and it's
        //     complex enough already.
        public ChannelPipeline getPipeline() throws Exception {
            final DynamicProtobufDecoder protbufDecoder =
                    new DynamicProtobufDecoder();
            final ChannelPipeline p = pipeline();
            decoderMap.put(p, protbufDecoder);
            p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
            p.addLast("protobufDecoder", protbufDecoder);
            p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
            p.addLast("protobufEncoder", new ProtobufEncoder());
            final RpcPeerHandler handler = new RpcPeerHandler(protbufDecoder);
            if (listener != null) {
                handler.setListener(listener);
            }
            p.addLast("handler", handler);

            putNextPrototype(p, Rpc.RpcControl.getDefaultInstance());
            return p;
        }
    }

    public void setUpServer(final RpcMessageListener listener) {

        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

       final RpcPeerPipelineFactory serverChannelPipelineFactory =
               new RpcPeerPipelineFactory("server", listener);

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);


        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(PORT));

    }

    private void setUpClient() {

        // Configure the client.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        clientBootstrap.setPipelineFactory(
                clientPipelineFactory);

        // Start the connection attempt.
        final ChannelFuture future =
                clientBootstrap.connect(new InetSocketAddress(HOST, PORT));

        // Wait until the connection is closed or the connection attempt fails.
        future.getChannel().getCloseFuture().awaitUninterruptibly();

        // Shut down thread pools to exit.
        clientBootstrap.releaseExternalResources();
    }



    @Test
    public void testTransmission() {

        final RpcMessageListener ml = new RpcMessageListener() {
            @Override
            public void receiveMessage(
                    final Object message,
                    final ChannelHandlerContext ctx) {
                if (message instanceof Rpc.RpcControl) {
                    Rpc.RpcControl msg = (Rpc.RpcControl) message;
                    serverControlReceiver.receive(msg);

                    ctx.getChannel().close();
                }
            }
        };

        setUpServer(ml);
        setUpClient();

        verify(serverControlReceiver).receive(heartbeatMessage);
    }
}
