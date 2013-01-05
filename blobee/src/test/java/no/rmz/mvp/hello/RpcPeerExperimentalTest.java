package no.rmz.mvp.hello;

import java.util.logging.Logger;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelHandlerContext;
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
    private final static Rpc.RpcControl HEARTBEAT_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();

    // We need an interface to receive something into a mock
    // and this is it.
    public interface Receiver<T> {

        public void receive(final T param);
    }
    @Mock
    Receiver<Rpc.RpcControl> serverControlReceiver;

    @Test
    public void testTransmission() {

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

        RpcSetup.setUpServer(PORT, ml);
        RpcSetup.setUpClient(HOST, PORT, ml);

        verify(serverControlReceiver, times(2)).receive(HEARTBEAT_MESSAGE);
    }
}
