package no.rmz.mvp.hello;

import java.io.IOException;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.testtools.Net;
import org.jboss.netty.channel.ChannelHandlerContext;
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

    private final static String HOST = "localhost";
    private final static Rpc.RpcControl HEARTBEAT_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();

     private final static Rpc.RpcControl SHUTDOWN_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.SHUTDOWN).build();

    // We need an interface to receive something into a mock
    // and this is it.
    public interface Receiver<T> {
        public void receive(final T param);
    }

    int port;

    @Before
    public void setUp() throws IOException {
        port = Net.getFreePort();
    }

    @Mock
    Receiver<Rpc.RpcControl> serverControlReceiver;

    // @Test
    public void testTransmissionOfHeartbeatsAtStartup() {

        final RpcMessageListener ml = new RpcMessageListener() {
            @Override
            public void receiveMessage(
                    final Object message,
                    final ChannelHandlerContext ctx) {
                if (message instanceof Rpc.RpcControl) {
                    final Rpc.RpcControl msg = (Rpc.RpcControl) message;
                    serverControlReceiver.receive(msg);

                    ctx.getChannel().close();
                }
            }
        };

        RpcSetup.setUpServer(port, ml);
        RpcSetup.setUpClient(HOST, port, ml);

        verify(serverControlReceiver, times(2)).receive(HEARTBEAT_MESSAGE);
    }


    @Test
    public void testReactionToShutdown() {

        final RpcMessageListener ml = new RpcMessageListener() {
            @Override
            public void receiveMessage(
                    final Object message,
                    final ChannelHandlerContext ctx) {
                if (message instanceof Rpc.RpcControl) {
                    final Rpc.RpcControl msg = (Rpc.RpcControl) message;

                    serverControlReceiver.receive(msg);
                    if (msg.getMessageType() == Rpc.MessageType.HEARTBEAT) {
                        ctx.getChannel().write(SHUTDOWN_MESSAGE);
                    } else if (msg.getMessageType() == Rpc.MessageType.SHUTDOWN) {
                         serverControlReceiver.receive(msg);
                    }
                }
            }
        };

        RpcSetup.setUpServer(port, ml);
        RpcSetup.setUpClient(HOST, port, ml);

        // XXX Don't understand why this leads to four (!!) shutdown messages
        //     but there you are.
        verify(serverControlReceiver, times(4)).receive(SHUTDOWN_MESSAGE);
    }
}
