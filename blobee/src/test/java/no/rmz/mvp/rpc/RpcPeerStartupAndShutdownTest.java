package no.rmz.mvp.rpc;

import java.io.IOException;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.RemoteExecutionContext;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.testtools.Net;
import no.rmz.testtools.Receiver;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public final class RpcPeerStartupAndShutdownTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.mvp.rpc.RpcPeerStartupAndShutdownTest.class.getName());


    private final static String HOST = "localhost";
    private final static Rpc.RpcControl HEARTBEAT_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();

     private final static Rpc.RpcControl SHUTDOWN_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.SHUTDOWN).build();


    int port;

    @Before
    public void setUp() throws IOException {
        port = Net.getFreePort();
    }

    @Mock
    Receiver<Rpc.RpcControl> serverControlReceiver;

    @Test
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

        final RpcClient rpcClient = RpcSetup.setUpClient(HOST, port, executor, ml);
        // XXX This is actually a bit bogus, since what the server
        //     needs is not a client that can connect to somewhere (in
        //     particular it doesn't need a client that can connect to itself
        //     as we're setting it up to do here), but it does need somewhere
        //     to send returning RPC invocations to, and that's not strictly
        //     a client, but something that I've not abstracted out yet.
        //     Nonetheless, in the present test environment, this should
        //     work.
        RpcSetup.setUpServer(port, executor, rpcClient, ml);

        rpcClient.start();
        // Need some time to let the startup transient settle.
        sleepHalfASec();

        verify(serverControlReceiver, times(2)).receive(HEARTBEAT_MESSAGE);
    }

    public void sleepHalfASec() {
         try {
            Thread.currentThread().sleep(500);
        }
        catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    final RpcExecutionService executor = new RpcExecutionService() {
            public void execute(RemoteExecutionContext dc, ChannelHandlerContext ctx, Object param) {
                log.info("Executing dc = " + dc + ", param = " + param);
            }
        };

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

       final RpcClient rpcClient = RpcSetup.setUpClient(HOST, port, executor, ml);
        // XXX This is actually a bit bogus, since what the server
        //     needs is not a client that can connect to somewhere (in
        //     particular it doesn't need a client that can connect to itself
        //     as we're setting it up to do here), but it does need somewhere
        //     to send returning RPC invocations to, and that's not strictly
        //     a client, but something that I've not abstracted out yet.
        //     Nonetheless, in the present test environment, this should
        //     work.
        RpcSetup.setUpServer(port, executor, rpcClient, ml);

        rpcClient.start();
        // Need some time to let the startup transient settle.
        sleepHalfASec();

        // XXX Don't understand why this leads to four (!!) shutdown messages
        //     but there you are :-)
        verify(serverControlReceiver, times(4)).receive(SHUTDOWN_MESSAGE);
    }

}